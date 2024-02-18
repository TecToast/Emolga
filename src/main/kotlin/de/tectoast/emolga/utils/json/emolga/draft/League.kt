package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.draft.AddToTierlistData
import de.tectoast.emolga.features.draft.TipGame
import de.tectoast.emolga.features.flo.SendFeatures
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.draft.DraftPlayer
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.draft.TierlistMode
import de.tectoast.emolga.utils.json.LeagueResult
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.showdown.AnalysisData
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.EmbedBuilder
import dev.minn.jda.ktx.messages.SendDefaults
import dev.minn.jda.ktx.util.SLF4J
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import org.bson.types.ObjectId
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.updateOne
import org.slf4j.Logger
import java.text.SimpleDateFormat
import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.Delegates


@Suppress("MemberVisibilityCanBePrivate")
@Serializable
sealed class League {

    @SerialName("_id")
    @Contextual
    val id: ObjectId? = null
    val sid: String = "yay"
    val leaguename: String = "ERROR"
    var isRunning: Boolean = false
    val picks: MutableMap<Long, MutableList<DraftPokemon>> = mutableMapOf()
    val battleorder: MutableMap<Int, List<List<Int>>> = mutableMapOf()
    val allowed: MutableMap<Long, MutableSet<AllowedData>> = mutableMapOf()
    val guild = -1L
    var round = 1
    var current = -1L
    val timerRelated = TimerRelated()
    var pseudoEnd = false

    abstract val teamsize: Int
    open val gamedays: Int by lazy { battleorder.let { if (it.isEmpty()) table.size - 1 else it.size } }

    @Transient
    val points: PointsManager = PointsManager()
    val noAutoStart = false
    val timerStart: Long? = null

    @Transient
    open val afterTimerSkipMode: AfterTimerSkipMode? = null

    @Transient
    open val duringTimerSkipMode: DuringTimerSkipMode? = null

    val originalorder: Map<Int, List<Int>> = mapOf()

    val order: MutableMap<Int, MutableList<Int>> = mutableMapOf()
    val moved: MutableMap<Long, MutableList<Int>> = mutableMapOf()
    val skippedTurns: MutableMap<Long, MutableSet<Int>> = mutableMapOf()
    internal val names: MutableMap<Long, String> = mutableMapOf()


    val tc: TextChannel get() = jda.getTextChannelById(tcid) ?: error("No text channel found for guild $guild")

    var tcid: Long = -1


    @Transient
    private var tlCompanion = Tierlist // workaround

    val tierlist: Tierlist by tlCompanion

    @Transient
    open val pickBuffer = 0

    @Transient
    var newTimerForAnnounce = false

    @Transient
    open val alwaysSendTier = false




    val cooldownJob: Job? get() = allTimers[leaguename]
    val stallSecondJob: Job? get() = allStallSecondTimers[leaguename]

    @Transient
    open val allowPickDuringSwitch = false

    open var timer: DraftTimer? = null
    val isLastRound: Boolean get() = round == totalRounds
    val totalRounds by lazy { originalorder.size }

    var isSwitchDraft = false

    val table: List<Long> = listOf()

    val tipgame: TipGame? = null

    val tierorderingComparator by lazy { compareBy<DraftPokemon>({ tierlist.order.indexOf(it.tier) }, { it.name }) }


    val newSystemGap get() = teamsize + pickBuffer + 3

    open val additionalSet: AdditionalSet? by lazy { AdditionalSet((gamedays + 4).xc(), "X", "Y") }

    context (InteractionData)
    suspend fun lockForPick(data: BypassCurrentPlayerData, block: suspend () -> Unit) {
        getLock(leaguename).withLock {
            // this is only needed when timerSkipMode is AFTER_DRAFT_UNORDERED
            if (pseudoEnd && afterTimerSkipMode == AFTER_DRAFT_UNORDERED) {
                // BypassCurrentPlayerData can only be Yes here
                current = (data as BypassCurrentPlayerData.Yes).user
            }
            if (!isCurrentCheck(user)) {
                return@withLock reply("Du warst etwas zu langsam!", ephemeral = true)
            }
            block()
        }
    }

    fun RequestBuilder.newSystemPickDoc(data: DraftData) {
        val y = data.memIndex.y(newSystemGap, data.picks.size + 2)
        addSingle("$dataSheet!B$y", data.pokemon)
        additionalSet?.let {
            addSingle("$dataSheet!${it.col}$y", it.existent)
        }
    }

    fun RequestBuilder.newSystemSwitchDoc(data: SwitchData) {
        newSystemPickDoc(data)
        val y = data.memIndex.y(newSystemGap, data.oldIndex + 3)
        additionalSet?.let {
            addSingle("$dataSheet!${it.col}$y", it.yeeted)
        }
    }

    open fun onTipGameLockButtons(gameday: Int) {}
    open suspend fun AddToTierlistData.addMonToTierlist() {}

    open fun isFinishedForbidden() = !isSwitchDraft

    open fun checkFinishedForbidden(mem: Long): String? = null

    open fun savePick(picks: MutableList<DraftPokemon>, pokemon: String, tier: String, free: Boolean) {
        picks.add(DraftPokemon(pokemon, tier, free))
    }

    open fun saveSwitch(picks: MutableList<DraftPokemon>, oldmon: String, newmon: String, newtier: String): Int {
        picks.first { it.name == oldmon }.quit = true
        picks += DraftPokemon(newmon, newtier, false)
        return -1
    }

    open suspend fun RequestBuilder.pickDoc(data: PickData): Unit? = null

    open suspend fun RequestBuilder.switchDoc(data: SwitchData): Unit? = null

    open fun providePicksForGameday(gameday: Int): Map<Long, List<DraftPokemon>> = picks


    suspend fun isCurrentCheck(user: Long): Boolean {
        if (current == user || user in Constants.DRAFTADMINS) return true
        return isCurrent(user)
    }

    open suspend fun isCurrent(user: Long): Boolean {
        return allowed[current]?.any { it.u == user } ?: false
    }

    open suspend fun isPicked(mon: String, tier: String? = null) =
        picks.values.any { l -> l.any { !it.quit && it.name.equals(mon, ignoreCase = true) } }

    context (InteractionData)
    open fun handlePoints(
        tlNameNew: String, officialNew: String, free: Boolean, tier: String, tierOld: String? = null
    ): Boolean {
        if (!tierlist.mode.withPoints) return false
        val mega = officialNew.isMega
        if (tierlist.mode.isTiersWithFree() && !(tierlist.variableMegaPrice && mega) && !free) return false
        val cpicks = picks[current]!!
        val currentPicksHasMega = cpicks.any { it.name.isMega }
        if (tierlist.variableMegaPrice && currentPicksHasMega && mega) {
            reply("Du kannst nur ein Mega draften!")
            return true
        }
        val needed = tierlist.getPointsNeeded(tier)
        val pointsBack = tierOld?.let { tierlist.getPointsNeeded(it) } ?: 0
        if (points[current] - needed + pointsBack < 0) {
            reply("Dafür hast du nicht genug Punkte!")
            return true
        }
        val variableMegaPrice = (if (tierlist.variableMegaPrice) (if (!currentPicksHasMega) tierlist.order.mapNotNull {
            it.substringAfter("#", "").takeUnless { t -> t.isEmpty() }?.toInt()
        }.minOrNull() ?: 0 else 0) else 0).let {
            if (mega) 0 else it
        }
        if (pointsBack == 0 && when (tierlist.mode) {
                TierlistMode.POINTS -> (totalRounds - (cpicks.size + 1)) * tierlist.prices.values.min() > points[current] - needed
                TierlistMode.TIERS_WITH_FREE -> (tierlist.freePicksAmount - (cpicks.count { it.free } + (if (free) 1 else 0))) * tierlist.freepicks.entries.filter { it.key != "#AMOUNT#" && "Mega#" !in it.key }
                    .minOf { it.value } > points[current] - needed - variableMegaPrice

                else -> false
            }
        ) {
            reply("Wenn du dir dieses Pokemon holen würdest, kann dein Kader nicht mehr vervollständigt werden!")
            return true
        }
        points.add(current, pointsBack - needed)
        return false
    }

    context (InteractionData)
    open fun handleTiers(
        specifiedTier: String, officialTier: String, fromSwitch: Boolean = false
    ): Boolean {
        if (!tierlist.mode.withTiers || (tierlist.variableMegaPrice && "#" in officialTier)) return false
        val map = getPossibleTiers()
        if (!map.containsKey(specifiedTier)) {
            reply("Das Tier `$specifiedTier` existiert nicht!")
            return true
        }
        if (tierlist.order.indexOf(officialTier) < tierlist.order.indexOf(specifiedTier) && (!fromSwitch || map[specifiedTier]!! <= 0)) {
            reply("Du kannst ein $officialTier-Mon nicht ins $specifiedTier hochdraften!")
            return true
        }
        if (map[specifiedTier]!! <= 0) {
            if (tierlist.prices[specifiedTier] == 0) {
                reply("Ein Pokemon aus dem $specifiedTier-Tier musst du in ein anderes Tier hochdraften!")
                return true
            }
            if (fromSwitch) return false
            reply("Du kannst dir kein $specifiedTier-Pokemon mehr picken!")
            return true
        }
        return false
    }

    suspend fun afterPickOfficial(data: NextPlayerData = NextPlayerData.Normal) {
        (duringTimerSkipMode?.takeIf { !draftWouldEnd } ?: afterTimerSkipMode)?.apply {
            if (afterPickCall(data).also { save("AfterPickOfficial") }) nextPlayer(data)
        } ?: nextPlayer(data)
    }


    val draftWouldEnd get() = isLastRound && order[round]!!.isEmpty()

    private fun MutableList<Int>.nextCurrent() = table[this.removeAt(0)]

    suspend fun startDraft(tc: GuildMessageChannel?, fromFile: Boolean, switchDraft: Boolean?) {
        switchDraft?.let { this.isSwitchDraft = it }
        logger.info("Starting draft $leaguename...")
        logger.info(tcid.toString())
        if (names.isEmpty()) {
            names.putAll(if (table.any { it < 11_000_000_000 }) table.associateWith { "${it - 10_000_000_000}" } else jda.getGuildById(
                this.guild
            )!!.retrieveMembersByIds(table).await().associate { it.idLong to it.effectiveName })
        }
        logger.info(names.toString())
        tc?.let { this.tcid = it.idLong }
        for (member in table) {
            if (fromFile || isSwitchDraft) picks.putIfAbsent(member, mutableListOf())
            else picks[member] = mutableListOf()
        }
        val updates = mutableListOf<SetTo<*>>()
        val currentTimeMillis = System.currentTimeMillis()
        if (!fromFile) {
            order.clear()
            order.putAll(originalorder.mapValues { it.value.toMutableList() })
            round = 1
            setNextUser()
            moved.clear()
            pseudoEnd = false
            timerRelated.lastPick = currentTimeMillis
            reset(updates)
            restartTimer()
            sendRound()
            announcePlayer()
            save("StartDraft")
            updates += ::round setTo 1
            updates += ::moved setTo mutableMapOf()
            updates += ::pseudoEnd setTo false
            updates += ::skippedTurns setTo mutableMapOf()
            updates += League::timerRelated / TimerRelated::lastPick setTo currentTimeMillis
            updates += League::timerRelated / TimerRelated::usedStallSeconds setTo mutableMapOf()
        } else {

            val delayData = if (timerRelated.cooldown > 0) DelayData(
                timerRelated.cooldown,
                timerRelated.regularCooldown,
                currentTimeMillis
            ) else timer?.calc(
                this, currentTimeMillis
            )
            restartTimer(delayData)
        }
        updates += ::isRunning setTo true
        db.drafts.updateOneById(id!!, set(*updates.toTypedArray()))
        logger.info("Started!")
    }

    fun setNextUser() {
        current = order[round]!!.nextCurrent()
    }

    suspend fun save(from: String = "") {
        val l = this@League
        db.drafts.updateOne(l).also {
            logger.info(
                "Saving... Result: {} Leaguename: {} isRunning {} FROM {}",
                it.toString(),
                l.leaguename,
                l.isRunning,
                from
            )
        }
    }

    override fun toString() = leaguename

    open fun reset(updates: MutableList<SetTo<*>>) {}

    private fun restartTimer(delayData: DelayData? = timer?.calc(this)) {
        val skipDelay = delayData?.skipDelay
        with(timerRelated) {
            skipDelay ?: run {
                cooldown = 0
                regularCooldown = 0
                return
            }
            afterTimerSkipMode?.run {
                if (disableTimer()) {
                    cancelCurrentTimer()
                    cooldown = 0
                    regularCooldown = 0
                    return
                }
            }
            lastRegularDelay = delayData.regularDelay
            cooldown = delayData.skipTimestamp
            regularCooldown = delayData.regularTimestamp
            newTimerForAnnounce = true
            logger.info("important".marker, "cooldown = {}", cooldown)
            cancelCurrentTimer("Restarting timer")
            allTimers[leaguename] = timerScope.launch {
                delay(skipDelay)
                withContext(NonCancellable) {
                    executeTimerOnRefreshedVersion(this@League.leaguename)
                }
            }
            timer?.stallSeconds?.takeIf { it > 0 }?.let {
                allStallSecondTimers[leaguename] = timerScope.launch {
                    val regularDelay = delayData.regularDelay
                    if (delayData.hasStallSeconds && regularDelay >= 0) {
                        delay(regularDelay)
                        lastStallSecondUsedMid = handleStallSecondUsed()
                        save("StallSecondAnnounce")
                    }
                }
            }
        }
    }

    open suspend fun handleStallSecondUsed(): Long? = null

    private fun sendRound() {
        tc.sendMessage("**=== Runde $round ===**").queue()
    }

    open suspend fun announcePlayer(data: NextPlayerData = NextPlayerData.Normal) {
        val currentMention = getCurrentMention()
        val announceData = announceData()
        with(data) {
            if (this is NextPlayerData.Moved) sendSkipMessage()
            tc.sendMessage("$currentMention ist dran!$announceData").queue()
        }
    }

    open fun NextPlayerData.Moved.sendSkipMessage() {
        val skippedUserName = getCurrentName(skippedUser)
        tc.sendMessage(if (reason == SkipReason.REALTIMER) "**$skippedUserName** war zu langsam!**"
        else "Der Pick von $skippedUserName wurde ".condAppend(skippedBy != null) { "von <@$skippedBy> " } + "${if (isSwitchDraft) "geskippt" else "verschoben"}!")
            .queue()
    }

    private fun announceData() = buildList {
        with(tierlist.mode) {
            if (withTiers) {
                getPossibleTiersAsString().let { if (it.isNotEmpty()) add("Mögliche Tiers: $it") }
            }
            if (withPoints) add(
                "${points[current]} mögliche Punkte".condAppend(
                    isTiersWithFree(), " für Free-Picks"
                )
            )
        }
    }.joinToString(prefix = " (", postfix = ")").let { if (it.length == 3) "" else it }
        .condAppend(newTimerForAnnounce) {
            " — Zeit bis: **${
                timeFormat.format(
                    timerRelated.regularCooldown
                )
            }**"
        }.also { newTimerForAnnounce = false }/*.condAppend(timerSkipMode?.multiplePicksPossible == true && hasMovedTurns()) {
        movedTurns().size.plus(1).let { " **($it Pick${if (it == 1) "" else "s"})**" }
    }*/

    open fun beforePick(): String? = null
    open fun beforeSwitch(): String? = null
    open fun checkUpdraft(specifiedTier: String, officialTier: String): String? = null

    fun getPossibleTiers(mem: Long = current, forAutocomplete: Boolean = false): MutableMap<String, Int> {
        val cpicks = picks[mem]!!
        return tierlist.prices.toMutableMap().let { possible ->
            cpicks.forEach { pick ->
                pick.takeUnless { it.name == "???" || it.free || it.quit }
                    ?.let { possible[it.tier] = possible[it.tier]!! - 1 }
            }
            possible
        }.also { possible ->
            if (tierlist.variableMegaPrice) {
                possible.keys.toList().forEach { if (it.startsWith("Mega#")) possible.remove(it) }
                if (!forAutocomplete) possible["Mega"] = if (cpicks.none { it.name.isMega }) 1 else 0
            }
            manipulatePossibleTiers(cpicks, possible)
        }
    }

    open fun manipulatePossibleTiers(picks: MutableList<DraftPokemon>, possible: MutableMap<String, Int>) {}

    fun getPossibleTiersAsString(mem: Long = current) =
        getPossibleTiers(mem).entries.sortedBy { it.key.indexedBy(tierlist.order) }.filterNot { it.value == 0 }
            .joinToString { "${it.value}x **".condAppend(it.key.toIntOrNull() != null, "Tier ") + "${it.key}**" }
            .let { str ->
                if (tierlist.mode.isTiersWithFree()) str + "; ${tierlist.freePicksAmount - picks[mem]!!.count { it.free }}x **Free Pick**"
                else str
            }

    suspend fun getTierOf(pokemon: String, insertedTier: String?): TierData? {
        val real = tierlist.getTierOf(pokemon) ?: return null
        return if (insertedTier != null && tierlist.mode.withTiers) {
            TierData(tierlist.order.firstOrNull {
                insertedTier.equals(
                    it, ignoreCase = true
                )
            } ?: (if (tierlist.variableMegaPrice && insertedTier.equals("Mega", ignoreCase = true)) "Mega" else ""),
                real)
        } else {
            TierData(real, real)
        }
    }


    fun PickData.getTierInsertIndex(): Int {
        var index = 0
        for (entry in tierlist.prices.entries) {
            if (entry.key == this.tier) {
                return this.picks.count { !it.free && it.tier == this.tier } + index - 1
            }
            index += entry.value
        }
        error("Tier ${this.tier} not found by user $current")
    }

    fun triggerMove() {
        if (!isSwitchDraft) moved.getOrPut(current) { mutableListOf() }.let { if (round !in it) it += round }
        skippedTurns.getOrPut(current) { mutableSetOf() } += round
    }

    fun hasMovedTurns(user: Long = current) = movedTurns(user).isNotEmpty()
    fun movedTurns(user: Long = current) = moved[user] ?: mutableListOf()

    private suspend fun endOfTurn(): Boolean {
        logger.info("End of turn")
        if (order[round]!!.isEmpty()) {
            logger.info("No more players")
            if (round == totalRounds) {

                finishDraft(msg = "Der Draft ist vorbei!")
                return true
            }
            round++
            onRoundSwitch()
            if (order[round]?.isEmpty() != false) {
                finishDraft(msg = "Da alle bereits ihre Drafts beendet haben, ist der Draft vorbei!")
                return true
            }
            sendRound()
        }
        return false
    }

    open suspend fun onRoundSwitch() {}

    suspend fun finishDraft(msg: String) {
        tc.sendMessage(msg).queue()
        //ndsdoc(tierlist, pokemon, d, mem, tier, round);
        //aslCoachDoc(tierlist, pokemon, d, mem, needed, round, null);
        logger.info("Draft ended!")
        isRunning = false
        logger.info("Draft isRunning {}", isRunning)
        logger.info("Saving......... $leaguename")
        save("END SAVE")
        logger.info("Saved!")
        db.drafts.updateOneById(id!!, set(League::isRunning setTo false))
        logger.info("SAVED SEPARATELY")
    }

    internal open suspend fun getCurrentMention(): String {
        val data = allowed[current] ?: return "<@$current>"
        val currentData = data.firstOrNull { it.u == current } ?: AllowedData(current, true)
        val (teammates, other) = data.filter { it.mention && it.u != current }.partition { it.teammate }
        return (if (currentData.mention) "<@$current>" else "**${getCurrentName()}**") + teammates.joinToString { "<@${it.u}>" }
            .ifNotEmpty { ", $it" } + other.joinToString { "<@${it.u}>" }.ifNotEmpty { ", ||$it||" }
    }

    internal fun getCurrentName(mem: Long = current) = names[mem]!!

    fun indexInRound(round: Int): Int = originalorder[round]!!.indexOf(current.indexedBy(table))

    fun cancelCurrentTimer(reason: String = "Next player") {
        cooldownJob?.cancel(reason)
        stallSecondJob?.cancel(reason)
    }

    private suspend fun nextPlayer(data: NextPlayerData = NextPlayerData.Normal) = with(timerRelated) {
        if (!isRunning) return
        val ctm = System.currentTimeMillis()
        timer?.stallSeconds?.let {
            if (cooldown > 0) {
                val punishSeconds = ((ctm - lastPick - lastRegularDelay) / 1000).toInt()
                if (punishSeconds > 0) usedStallSeconds.add(current, punishSeconds)
            }
        }
        when (data) {
            is NextPlayerData.Normal -> cancelCurrentTimer()
            is NextPlayerData.Moved -> {
                triggerMove()
                data.skippedUser = current
            }
        }
        if (endOfTurn()) return
        lastPick = ctm
        onNextPlayer(data)
        setNextUser()
        restartTimer()
        announcePlayer(data)
        lastStallSecondUsedMid = 0
        save("NEXT PLAYER SAFE")
    }

    open suspend fun onNextPlayer(data: NextPlayerData) {}

    fun addFinished(mem: Long) {
        val index = mem.indexedBy(table)
        order.values.forEach { it.remove(index) }
    }

    @Transient
    open val docEntry: DocEntry? = null

    @Transient
    open val dataSheet: String = "Data"

    fun builder() = RequestBuilder(sid)
    context (InteractionData)
    suspend fun replyPick(pokemon: String, free: Boolean, tier: String, updrafted: Boolean) =
        replyGeneral("$pokemon ".condAppend(alwaysSendTier || updrafted) { "im $tier " } + "gepickt!".condAppend(
            updrafted
        ) { " (Hochgedraftet)" }.condAppend(free) { " (Free-Pick, neue Punktzahl: ${points[current]})" })

    context (InteractionData)
    suspend fun replyGeneral(msg: String, components: Collection<LayoutComponent> = SendDefaults.components) {
        replyAwait(
            "<@${user}> hat${
                if (user != current) " für **${getCurrentName()}**" else ""
            } $msg", components = components
        )
    }

    context (InteractionData)
    suspend fun replyRandomPick(pokemon: String, tier: String) = replyGeneral(
        "einen Random-Pick im $tier gemacht und **$pokemon** bekommen!"
    )

    context (InteractionData)
    suspend fun replySwitch(oldmon: String, newmon: String) {
        replyGeneral("$oldmon gegen $newmon getauscht!")
    }

    context (InteractionData)
    suspend fun replySkip() {
        replyGeneral("den Pick übersprungen!")
    }

    suspend fun getPickRoundOfficial() =
        afterTimerSkipMode?.run { getPickRound().also { save("GET PICK ROUND") } } ?: round

    open fun provideReplayChannel(jda: JDA): TextChannel? = null
    open fun provideResultChannel(jda: JDA): TextChannel? = null

    open fun appendedEmbed(data: AnalysisData, league: LeagueResult, gdData: GamedayData) = EmbedBuilder {
        val game = data.game
        val p1 = game[0].nickname
        val p2 = game[1].nickname
        title = "${data.ctx.format} replay: $p1 vs. $p2"
        url = data.ctx.url.takeIf { it.length > 10 } ?: "https://example.org"
        description = "Spieltag ${gdData.gameday}: " + league.uids.joinToString(" vs. ") { "<@$it>" }
    }

    /**
     * generate the gameplan coords
     * @param u1 the first user
     * @param u1 the second user
     * @return a triple containing the gameday, the battle index and if p1 is the second user
     */
    private fun gameplanCoords(u1: Long, u2: Long): Triple<Int, Int, Boolean> {
        val size = table.size
        val numDays = size - 1
        val halfSize = size / 2
        val list = table.run { listOf(indexOf(u1), indexOf(u2)) }
        for (day in 0 until numDays) {
            val teamIdx = day % numDays + 1
            if (0 in list) {
                if (list[1 - list.indexOf(0)] == teamIdx) return Triple(day + 1, 0, list[0] == 0)
                continue
            }
            for (idx in 1 until halfSize) {
                val firstTeam = (day + idx) % numDays + 1
                val secondTeam = (day + numDays - idx) % numDays + 1
                if (firstTeam in list) {
                    if (list[1 - list.indexOf(firstTeam)] == secondTeam) return Triple(
                        day + 1, idx, list[0] == secondTeam
                    )
                    break
                }
            }
        }
        error("Didnt found matchup for $u1 & $u2 in $leaguename")
    }

    fun getGameplayData(uid1: Long, uid2: Long, game: List<DraftPlayer>): GamedayData {
        var battleind = -1
        var u1IsSecond = false
        val i1 = table.indexOf(uid1)
        val i2 = table.indexOf(uid2)
        val gameday = if (battleorder.isNotEmpty()) battleorder.asIterable().reversed().firstNotNullOfOrNull {
            if (it.value.any { l ->
                    l.containsAll(listOf(i1, i2)).also { b ->
                        if (b) u1IsSecond = l.indexOf(i1) == 1
                    }
                }) it.key else null
        } ?: -1 else gameplanCoords(uid1, uid2).also {
            battleind = it.second
            u1IsSecond = it.third
        }.first
        val indices = listOf(i1, i2)
        val (battleindex, numbers) = battleorder[gameday]?.let { battleorder ->
            val battleusers = battleorder.firstOrNull { it.contains(i1) }.orEmpty()
            (battleorder.indices.firstOrNull { battleorder[it].contains(i1) } ?: -1) to {
                (0..1).asSequence().sortedBy { battleusers.indexOf(indices[it]) }.map { game[it].alivePokemon }.toList()
            }
        } ?: run {
            battleind to {
                (0..1).map { game[it].alivePokemon }.let { if (u1IsSecond) it.reversed() else it }
            }
        }
        return GamedayData(
            gameday, battleindex, u1IsSecond, numbers
        )
    }

    inner class PointsManager {
        private val points = mutableMapOf<Long, Int>()

        operator fun get(member: Long) = points.getOrPut(member) {
            val isPoints = tierlist.mode.isPoints()
            tierlist.points - picks[member]!!.filterNot { it.quit }.sumOf {
                if (it.free) tierlist.freepicks[it.tier]!! else if (isPoints) tierlist.prices.getValue(
                    it.tier
                ) else if (tierlist.variableMegaPrice && it.name.isMega) it.tier.substringAfter("#").toInt() else 0
            }
        }

        fun add(member: Long, points: Int) {
            this.points[member] = this[member] + points
        }
    }

    val timeFormat get() = if (timer?.stallSeconds == 0) leagueTimeFormat else leagueTimeFormatSecs

    companion object {
        val logger: Logger by SLF4J
        val allTimers = mutableMapOf<String, Job>()
        val allStallSecondTimers = mutableMapOf<String, Job>()
        val leagueTimeFormat = SimpleDateFormat("HH:mm")
        val leagueTimeFormatSecs = SimpleDateFormat("HH:mm:ss")
        val allMutexes = ConcurrentHashMap<String, Mutex>()

        val timerScope = createCoroutineScope("LeagueTimer")

        fun getLock(leaguename: String): Mutex = allMutexes.getOrPut(leaguename) { Mutex() }

        suspend fun executeTimerOnRefreshedVersion(name: String) {
            getLock(name).withLock {
                val league = db.drafts.findOne(League::leaguename eq name)
                    ?: return SendFeatures.sendToMe("League $name not found")
                if (league.timerRelated.cooldown <= System.currentTimeMillis()) league.afterPickOfficial(
                    data = NextPlayerData.Moved(
                        SkipReason.REALTIMER
                    )
                )
            }
        }

        context (InteractionData)
        suspend fun byCommand(): Pair<League, BypassCurrentPlayerData>? {
            val onlyChannel = onlyChannel(tc)
            logger.info("leaguename {}", onlyChannel?.leaguename)
            return onlyChannel?.run {
                val uid = user
                if (pseudoEnd) {
                    val data = afterTimerSkipMode?.run { bypassCurrentPlayerCheck(uid) }
                    when (data) {
                        is BypassCurrentPlayerData.Yes -> {
                            return@run this to data
                        }

                        BypassCurrentPlayerData.No -> {
                            reply("Du hast keine offenen Picks mehr!")
                            return null
                        }

                        else -> {}
                    }
                }
                if (!isCurrentCheck(uid)) {
                    reply("Du bist nicht dran!", ephemeral = true)
                    return null
                }
                this to BypassCurrentPlayerData.No
            }
        }

        suspend fun onlyChannel(tc: Long) = db.drafts.find(League::isRunning eq true, League::tcid eq tc).first()
    }


}

@Serializable
data class TimerRelated(
    var cooldown: Long = -1,
    var regularCooldown: Long = -1,
    var lastPick: Long = -1,
    var lastRegularDelay: Long = -1,
    val usedStallSeconds: MutableMap<Long, Int> = mutableMapOf(),
    var lastStallSecondUsedMid: Long? = null
)

val String.isMega get() = "-Mega" in this

@Serializable
data class AllowedData(val u: Long, var mention: Boolean = false, var teammate: Boolean = false)

sealed class DraftData(
    open val league: League,
    open val pokemon: String,
    open val pokemonofficial: String,
    open val tier: String,
    open val mem: Long,
    open val indexInRound: Int,
    open val changedIndex: Int,
    open val picks: MutableList<DraftPokemon>,
    open val round: Int,
    open val memIndex: Int,
) {
    val roundIndex get() = round - 1
    abstract val changedOnTeamsiteIndex: Int
}

@Suppress("unused")
data class PickData(
    override val league: League,
    override val pokemon: String,
    override val pokemonofficial: String,
    override val tier: String,
    override val mem: Long,
    override val indexInRound: Int,
    override val changedIndex: Int,
    override val picks: MutableList<DraftPokemon>,
    override val round: Int,
    override val memIndex: Int,
    val freePick: Boolean
) : DraftData(league, pokemon, pokemonofficial, tier, mem, indexInRound, changedIndex, picks, round, memIndex) {
    override val changedOnTeamsiteIndex: Int by lazy {
        with(league) { getTierInsertIndex() }
    }
}


class SwitchData(
    league: League,
    pokemon: String,
    pokemonofficial: String,
    tier: String,
    mem: Long,
    indexInRound: Int,
    changedIndex: Int,
    picks: MutableList<DraftPokemon>,
    round: Int,
    memIndex: Int,
    val oldmon: String,
    val oldIndex: Int,
    override val changedOnTeamsiteIndex: Int
) : DraftData(league, pokemon, pokemonofficial, tier, mem, indexInRound, changedIndex, picks, round, memIndex)

data class TierData(val specified: String, val official: String)

sealed interface TimerSkipMode {
    suspend fun League.afterPickCall(data: NextPlayerData): Boolean

    /**
     * What happens after a pick/timer skip
     * @param data the data of the pick
     * @return if the next player should be announced
     */
    suspend fun League.afterPick(data: NextPlayerData): Boolean
    suspend fun League.getPickRound(): Int

    suspend fun League.bypassCurrentPlayerCheck(user: Long): BypassCurrentPlayerData = BypassCurrentPlayerData.No
    fun League.disableTimer() = false
}

sealed interface BypassCurrentPlayerData {
    data object No : BypassCurrentPlayerData
    data class Yes(val user: Long) : BypassCurrentPlayerData
}

interface DuringTimerSkipMode : TimerSkipMode {
    override suspend fun League.afterPickCall(data: NextPlayerData) = afterPick(data)
}

interface AfterTimerSkipMode : TimerSkipMode {
    override suspend fun League.afterPickCall(data: NextPlayerData) = if (draftWouldEnd) afterPick(data) else true
}

@Serializable
data object LAST_ROUND : DuringTimerSkipMode {
    override suspend fun League.afterPick(data: NextPlayerData): Boolean {
        if (isLastRound && hasMovedTurns()) {
            announcePlayer()
            return false
        }
        return true
    }

    override suspend fun League.getPickRound(): Int = round.let {
        if (it != totalRounds) it
        else {
            val mt = movedTurns()
            if (totalRounds - picks[current]!!.size < mt.size) {
                mt.removeFirst()
            } else it
        }
    }

}

@Serializable
data object NEXT_PICK : DuringTimerSkipMode {
    override suspend fun League.afterPick(data: NextPlayerData) =
        if (data !is NextPlayerData.Moved && hasMovedTurns()) {
            movedTurns().removeFirstOrNull()
            announcePlayer()
            false
        } else true


    override suspend fun League.getPickRound(): Int = movedTurns().firstOrNull() ?: round
}

@Serializable
data object AFTER_DRAFT_ORDERED : AfterTimerSkipMode {
    override suspend fun League.afterPick(data: NextPlayerData): Boolean {
        populateAfterDraft()
        return true
    }


    override suspend fun League.getPickRound() = if (pseudoEnd) movedTurns().removeFirst()
    else round

    private fun League.populateAfterDraft() {
        val order = order[totalRounds]!!
        for (i in 1..totalRounds) {
            moved.forEach { (user, turns) -> if (i in turns) order.add(user.indexedBy(table)) }
        }
        if (order.size > 0) {
            pseudoEnd = true
        }
    }
}

@Serializable
data object AFTER_DRAFT_UNORDERED : AfterTimerSkipMode {
    override suspend fun League.afterPick(data: NextPlayerData): Boolean = if (moved.values.any { it.isNotEmpty() }) {
        if (!pseudoEnd) {
            tc.sendMessage("Der Draft wäre jetzt vorbei, aber es gibt noch Spieler, die keinen vollständigen Kader haben! Diese können nun in beliebiger Reihenfolge ihre Picks nachholen. Dies sind:\n" + moved.entries.filter { it.value.isNotEmpty() }
                .joinToString("\n") { (user, turns) -> "<@$user>: ${turns.size}x" }).queue()
            cancelCurrentTimer()
            pseudoEnd = true
        }
        false
    } else true


    override suspend fun League.getPickRound(): Int = if (pseudoEnd) {
        movedTurns().removeFirst()
    } else round


    override suspend fun League.bypassCurrentPlayerCheck(user: Long): BypassCurrentPlayerData {
        val no = BypassCurrentPlayerData.No
        // Are we in the pseudo end?
        if (!pseudoEnd) return no
        // Has the user moved turns?
        if (hasMovedTurns(user)) return BypassCurrentPlayerData.Yes(user)
        // Is the user not in the table (for security reasons)?
        if (user in table) return no
        // Has the user permission for exactly one player?
        allowed.entries.singleOrNull { it.value.any { data -> data.u == user } }?.let { (u, _) ->
            if (hasMovedTurns(u)) return BypassCurrentPlayerData.Yes(u)
        }
        return no
    }

    override fun League.disableTimer(): Boolean {
        return pseudoEnd
    }
}


data class AdditionalSet(val col: String, val existent: String, val yeeted: String)
sealed interface NextPlayerData {
    data object Normal : NextPlayerData
    data class Moved(val reason: SkipReason, val skippedBy: Long? = null) : NextPlayerData {
        var skippedUser by Delegates.notNull<Long>()
    }
}

enum class SkipReason {
    REALTIMER, SKIP
}

data class GamedayData(
    val gameday: Int, val battleindex: Int, val u1IsSecond: Boolean
) {
    constructor(gameday: Int, battleindex: Int, u1IsSecond: Boolean, numbers: () -> List<Int>) : this(
        gameday, battleindex, u1IsSecond
    ) {
        this.numbers = numbers
    }

    lateinit var numbers: () -> List<Int>
}
