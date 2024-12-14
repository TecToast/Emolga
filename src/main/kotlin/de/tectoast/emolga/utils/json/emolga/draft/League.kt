@file:OptIn(ExperimentalSerializationApi::class)

package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.exposed.DraftName
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.features.ArgBuilder
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.TestInteractionData
import de.tectoast.emolga.features.draft.AddToTierlistData
import de.tectoast.emolga.features.draft.InstantToStringSerializer
import de.tectoast.emolga.features.draft.TipGame
import de.tectoast.emolga.features.draft.TipGameManager
import de.tectoast.emolga.features.flo.SendFeatures
import de.tectoast.emolga.league.DynamicCoord
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.draft.*
import de.tectoast.emolga.utils.draft.DraftUtils.executeWithinLock
import de.tectoast.emolga.utils.draft.Tierlist.Companion.getValue
import de.tectoast.emolga.utils.json.LeagueResult
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.showdown.AnalysisData
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant
import kotlinx.serialization.*
import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import org.bson.types.ObjectId
import org.litote.kmongo.coroutine.updateOne
import org.litote.kmongo.eq
import java.awt.Color
import java.text.SimpleDateFormat
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import kotlin.time.Duration


@OptIn(ExperimentalSerializationApi::class)
@Suppress("MemberVisibilityCanBePrivate")
@Serializable
sealed class League {
    @SerialName("_id")
    @Contextual
    val id: ObjectId? = null
    val sid: String = "yay"
    val leaguename: String = "ERROR"

    @EncodeDefault
    var isRunning: Boolean = false
    val picks: MutableMap<Int, MutableList<DraftPokemon>> = mutableMapOf()
    val battleorder: MutableMap<Int, List<List<Int>>> = mutableMapOf()
    val allowed: MutableMap<Int, MutableSet<AllowedData>> = mutableMapOf()
    val guild = -1L

    @EncodeDefault
    var round = 1
    var current = -1
    val timerRelated = TimerRelated()

    @EncodeDefault
    var pseudoEnd = false

    abstract val teamsize: Int
    open val gamedays: Int by lazy { battleorder.let { if (it.isEmpty()) table.size - 1 else it.size } }

    @Transient
    val points: PointsManager = PointsManager()
    val configs: MutableSet<LeagueConfig> = mutableSetOf()
    val noAutoStart = false
    val timerStart: Long? = null

    @Transient
    open val afterTimerSkipMode: AfterTimerSkipMode? = null

    @Transient
    open val duringTimerSkipMode: DuringTimerSkipMode? = null

    val originalorder: Map<Int, List<Int>> = mapOf()

    val order: MutableMap<Int, MutableList<Int>> = mutableMapOf()

    @EncodeDefault
    val moved: MutableMap<Int, MutableList<Int>> = mutableMapOf()

    @EncodeDefault
    val punishableSkippedTurns: MutableMap<Int, MutableSet<Int>> = mutableMapOf()
    internal val names: MutableList<String> = mutableListOf()

    val replayDataStore: ReplayDataStore? = null
    val ytSendChannel: Long? = null


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
    var lastPickedMon: DraftName? = null

    open var timer: DraftTimer? = null
    val isLastRound: Boolean get() = round == totalRounds
    val totalRounds by lazy { originalorder.size }

    var isSwitchDraft = false

    val table: List<Long> = ArrayList()

    val tipgame: TipGame? = null

    val tierorderingComparator by lazy { compareBy<DraftPokemon>({ tierlist.order.indexOf(it.tier) }, { it.name }) }


    val newSystemGap get() = teamsize + pickBuffer + 3

    open val additionalSet: AdditionalSet? by lazy { AdditionalSet((gamedays + 4).xc(), "X", "Y") }

    val queuedPicks: MutableMap<Int, QueuePicksData> = mutableMapOf()
    val randomLeagueData: RandomLeagueData = RandomLeagueData()

    @EncodeDefault
    val bannedMons: MutableMap<Int, MutableSet<DraftPokemon>> = mutableMapOf()

    protected fun enableConfig(vararg flags: LeagueConfig) {
        configs += flags.filter { f -> configs.none { f::class == it::class } }
    }


    inline fun <reified T : LeagueConfig> getConfig() = (configs.firstOrNull { it is T } as T?)
    inline fun <reified T : LeagueConfig> getConfigOrDefault() = getConfig<T>() ?: LeagueConfig.getDefaultConfig<T>()
    inline fun <reified T : LeagueConfig> config() = configs.any { it is T }
    inline fun <reified T : LeagueConfig> config(block: T.() -> Unit) {
        getConfig<T>()?.block()
    }


    fun index(mem: Long) = table.indexOf(mem)
    operator fun invoke(mem: Long) = table.indexOf(mem)
    operator fun get(index: Int) = table[index]

    suspend inline fun lock(block: League.() -> Unit) {
        getLock(leaguename).withLock {
            apply(block)
        }
    }

    fun RequestBuilder.newSystemPickDoc(data: DraftData, insertionIndex: Int = data.picks.size - 1): Int {
        val y = data.idx.y(newSystemGap, insertionIndex + 3)
        addSingle("$dataSheet!B$y", data.pokemon)
        additionalSet?.let {
            addSingle("$dataSheet!${it.col}$y", it.existent)
        }
        return y
    }

    fun RequestBuilder.newSystemSwitchDoc(data: SwitchData) {
        newSystemPickDoc(data)
        val y = data.idx.y(newSystemGap, data.oldIndex + 3)
        additionalSet?.let {
            addSingle("$dataSheet!${it.col}$y", it.yeeted)
        }
    }

    open suspend fun AddToTierlistData.addMonToTierlist() {}

    open fun setupRepeatTasks() {}

    open fun isFinishedForbidden() = !isSwitchDraft

    open fun checkFinishedForbidden(mem: Int): String? = null

    open fun PickData.savePick() {
        picks.add(DraftPokemon(pokemonofficial, tier, freePick))
    }

    open fun SwitchData.saveSwitch() {
        picks.first { it.name == oldmon.official }.quit = true
        picks += DraftPokemon(pokemonofficial, tier)
    }

    open fun BanData.saveBan() {
        bannedMons.getOrPut(round) { mutableSetOf() }.add(DraftPokemon(pokemonofficial, tier))
    }

    open suspend fun RequestBuilder.pickDoc(data: PickData) {}

    open suspend fun RequestBuilder.switchDoc(data: SwitchData) {}

    open suspend fun RequestBuilder.banDoc(data: BanData) {}

    open fun providePicksForGameday(gameday: Int): Map<Int, List<DraftPokemon>> = picks


    suspend fun isCurrentCheck(user: Long): Boolean {
        if (this[current] == user || user in Constants.DRAFTADMINS) return true
        return isCurrent(user)
    }

    open suspend fun isCurrent(user: Long): Boolean {
        return allowed[current]?.any { it.u == user } == true
    }

    open suspend fun isPicked(mon: String, tier: String? = null) =
        (picks.values + bannedMons.values).any { l -> l.any { !it.quit && it.name.equals(mon, ignoreCase = true) } }

    suspend inline fun firstAvailableMon(
        tlNames: Collection<String>, checker: ((german: String, english: String) -> Boolean) = { _, _ -> true }
    ): DraftName? {
        val alreadyPicked = (picks.values + bannedMons.values).flatten().mapTo(mutableSetOf()) { it.name }
        return tlNames.firstNotNullOfOrNull {
            val draftName = NameConventionsDB.getDiscordTranslation(
                it, guild, tierlist.isEnglish
            )!!
            if (draftName.official !in alreadyPicked && checker(
                    draftName.official, NameConventionsDB.getDiscordTranslation(
                        it, guild, english = true
                    )!!.official
                )
            ) draftName
            else null
        }
    }

    context (InteractionData)
    open fun handlePoints(
        free: Boolean, tier: String, tierOld: String? = null, mega: Boolean = false
    ): Boolean {
        if (!tierlist.mode.withPoints) return false
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
                TierlistMode.POINTS -> minimumNeededPointsForTeamCompletion(cpicks.size + 1) > points[current] - needed
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

    fun minimumNeededPointsForTeamCompletion(picksSizeAfterAdd: Int) =
        (totalRounds - picksSizeAfterAdd) * tierlist.prices.values.min()

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
        if (!isRunning) return
        randomLeagueData.currentMon?.disabled = true
        checkForQueuedPicksChanges()
        onAfterPick(data)
        val result = (duringTimerSkipMode?.takeIf { !draftWouldEnd } ?: afterTimerSkipMode)?.run {
            afterPickCall(data).also { save("AfterPickOfficial") }
        } ?: TimerSkipResult.NEXT
        val ctm = System.currentTimeMillis()
        timerRelated.lastPick = ctm
        timerRelated.lastStallSecondUsedMid = 0
        if (result != TimerSkipResult.SAME) {
            timerRelated.handleStallSecondPunishment(ctm)
            this@League.cancelCurrentTimer()
        }
        if (data is NextPlayerData.Moved) {
            addPunishableSkippedRound(data)
            getConfig<DraftBanConfig>()?.let { config ->
                if (config.skipBehavior == BanSkipBehavior.RANDOM) {
                    val data = config.banRounds[round] ?: return@let
                    val tier = data.getPossibleBanTiers(getAlreadyBannedMonsInThisRound()).random()
                    val possibleMons = tierlist.getByTier(tier) ?: return@let
                    val selectedMon = firstAvailableMon(possibleMons) ?: return@let
                    with(queueInteractionData) outer@{
                        executeWithinLock(BanInput(selectedMon), DraftMessageType.RANDOM)
                    }
                    return
                }
            }
            addToMoved(data)
            data.sendSkipMessage()
        }
        if (result == TimerSkipResult.NEXT) {
            if (endOfTurn()) return
            setNextUser()
        }
        if (result == TimerSkipResult.PSEUDOEND) {
            val randomOrder = moved.values.flatten().toSet().shuffled()
            for (idx in randomOrder) {
                if (tryQueuePick(idx)) break
            }
            return
        }

        if (tryQueuePick()) return


        restartTimer()
        announcePlayer()
        save("NEXT PLAYER SAFE")
    }

    fun getAlreadyBannedMonsInThisRound(): Set<DraftPokemon> = bannedMons[round].orEmpty()

    private fun TimerRelated.handleStallSecondPunishment(ctm: Long) {
        timer?.stallSeconds?.takeIf { it > 0 }?.let {
            if (cooldown > 0) {
                val punishSeconds = ((ctm - lastPick - lastRegularDelay) / 1000).toInt()
                if (punishSeconds > 0) usedStallSeconds.add(current, punishSeconds)
            }
        }
    }

    private suspend fun tryQueuePick(idx: Int = current): Boolean {
        val queuePicksData = queuedPicks[idx]?.takeIf { it.enabled } ?: return false
        val queuedMon = queuePicksData.queued.firstOrNull() ?: return false
        with(queueInteractionData) outer@{
            executeWithinLock(queuedMon.buildDraftInput(), type = DraftMessageType.QUEUE)
        }
        return true
    }

    private fun checkForQueuedPicksChanges() {
        val newMon = lastPickedMon ?: return
        queuedPicks.entries.filter { it.value.queued.any { mon -> mon.g == newMon } }.forEach { (mem, data) ->
            data.queued.removeIf { mon -> mon.g == newMon }
            if (mem != current) {
                SendFeatures.sendToUser(
                    table[mem], embeds = Embed(
                        title = "Queue-Pick-Warnung",
                        color = 0xff0000,
                        description = "`${newMon.tlName}` aus deiner Queue wurde gepickt.\n${if (data.disableIfSniped) "Das System wurde für dich deaktiviert, damit du umplanen kannst." else "Das System läuft jedoch für dich weiter."}"
                    ).into()
                )
                data.enabled = !data.disableIfSniped
            }
        }
        lastPickedMon = null
    }


    val draftWouldEnd get() = isLastRound && order[round]!!.isEmpty()

    suspend fun startDraft(
        tc: GuildMessageChannel?, fromFile: Boolean, switchDraft: Boolean?, nameGuildId: Long? = null
    ) {
        lock {
            switchDraft?.let { this.isSwitchDraft = it }
            logger.info("Starting draft $leaguename...")
            logger.info(tcid.toString())
            if (!fromFile) {
                names.clear()
                names.addAll(if (table.any { it < 11_000_000_000 }) table.map { "${it - 10_000_000_000}" } else jda.getGuildById(
                    nameGuildId ?: this.guild
                )!!.retrieveMembersByIds(table).await().sortedBy { it.idLong.indexedBy(table) }
                    .map { it.effectiveName })
            }
            logger.info(names.toString())
            tc?.let { this.tcid = it.idLong }
            for (idx in table.indices) {
                if (fromFile || isSwitchDraft) picks.putIfAbsent(idx, mutableListOf())
                else picks[idx] = mutableListOf()
            }
            isRunning = true
            val currentTimeMillis = System.currentTimeMillis()
            if (!fromFile) {
                order.clear()
                order.putAll(originalorder.mapValues { it.value.toMutableList() })
                round = 1
                moved.clear()
                pseudoEnd = false
                timerRelated.lastPick = currentTimeMillis
                timerRelated.usedStallSeconds.clear()
                punishableSkippedTurns.clear()
                bannedMons.clear()
                config<RandomPickConfig> {
                    if (hasJokers()) table.indices.forEach { randomLeagueData.jokers[it] = jokers }
                    randomLeagueData.currentMon?.disabled = true
                }
                reset()
                sendRound()
                if (tryQueuePick()) return
                restartTimer()
                announcePlayer()
                save("StartDraft")
            } else {
                val delayData = if (timerRelated.cooldown > 0) DelayData(
                    timerRelated.cooldown, timerRelated.regularCooldown, currentTimeMillis
                ) else timer?.calc(
                    this, currentTimeMillis
                )
                restartTimer(delayData)
            }
            logger.info("Started!")
        }
    }

    fun setNextUser() {
        current = order[round]!!.removeAt(0)
    }

    suspend fun save(from: String) {
        val l = this@League
        logger.debug { "Saving league from $from" }
        db.drafts.updateOne(l)
    }

    override fun toString() = leaguename

    open fun reset() {}

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
                    executeTimerOnRefreshedVersion(leaguename)
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
        tc.sendMessage("## === Runde $round ===").queue()
    }

    open suspend fun announcePlayer() {
        val currentMention = getCurrentMention()
        val announceData = announceData()
        tc.sendMessage("$currentMention ist dran!$announceData").queue()
    }

    open fun NextPlayerData.Moved.sendSkipMessage() {
        val skippedUserName = getCurrentName(skippedUser)
        val msg = if (reason == SkipReason.REALTIMER) "**$skippedUserName** war zu langsam!"
        else if (skippedBy != null) "Der Pick von $skippedUserName wurde von <@$skippedBy> ${if (isSwitchDraft) "geskippt" else "verschoben"}!"
        else null
        msg?.let { tc.sendMessage(it).queue() }
    }

    fun announceData(withTimerAnnounce: Boolean = true, idx: Int = current) = buildList {
        getConfig<DraftBanConfig>()?.let { config ->
            config.banRounds[round]?.let {
                add(
                    "Mögliche Tiers zum Bannen: ${
                        it.getPossibleBanTiers(getAlreadyBannedMonsInThisRound()).joinToString { s -> "**$s**" }
                    }")
                return@buildList
            }
        }
        with(tierlist.mode) {
            if (withTiers) {
                getPossibleTiersAsString(idx).let { if (it.isNotEmpty()) add("Mögliche Tiers: $it") }
            }
            if (withPoints) add(
                "${points[idx]} mögliche Punkte".condAppend(
                    isTiersWithFree(), " für Free-Picks"
                )
            )
        }
    }.joinToString(prefix = " (", postfix = ")").let { if (it.length == 3) "" else it }
        .condAppend(withTimerAnnounce && newTimerForAnnounce) {
            " — Zeit bis: **${
                timeFormat.format(
                    timerRelated.regularCooldown
                )
            }**"
        }.also { newTimerForAnnounce = false }

    open fun checkLegalDraftInput(input: DraftInput, type: DraftMessageType): String? {
        if (input is PickInput && type != DraftMessageType.RANDOM && getConfigOrDefault<RandomPickConfig>().hasJokers()) return "In diesem Draft sind keine regulären Picks möglich!"
        getConfig<DraftBanConfig>()?.let {
            it.banRounds[round]?.let {
                if (input !is BanInput) return "Die aktuelle Runde (**$round**) ist eine Ban-Runde, dementsprechend kann man nichts picken!"
            }
                ?: if (input is BanInput) return "Die aktuelle Runde (**$round**) ist **keine** Ban-Runde, dementsprechend kann man nichts bannen!" else Unit
        }
        if (input is PickInput && picks(current).count { !it.quit } >= teamsize) return "Dein Kader ist bereits voll!"
        return null
    }

    open fun checkUpdraft(specifiedTier: String, officialTier: String): String? = null

    fun getPossibleTiers(idx: Int = current, forAutocomplete: Boolean = false): MutableMap<String, Int> {
        val cpicks = picks[idx]
        return tierlist.prices.toMutableMap().let { possible ->
            cpicks?.forEach { pick ->
                pick.takeUnless { it.name == "???" || it.free || it.quit }
                    ?.let { possible[it.tier] = possible[it.tier]!! - 1 }
            }
            possible
        }.also { possible ->
            if (tierlist.variableMegaPrice) {
                possible.keys.toList().forEach { if (it.startsWith("Mega#")) possible.remove(it) }
                if (!forAutocomplete) possible["Mega"] = if (cpicks?.none { it.name.isMega } != false) 1 else 0
            }
            manipulatePossibleTiers(cpicks, possible)
        }
    }

    open fun manipulatePossibleTiers(picks: MutableList<DraftPokemon>?, possible: MutableMap<String, Int>) {}

    fun getPossibleTiersAsString(idx: Int = current) =
        getPossibleTiers(idx).entries.sortedBy { it.key.indexedBy(tierlist.order) }.filterNot { it.value == 0 }
            .joinToString { "${it.value}x **".condAppend(it.key.toIntOrNull() != null, "Tier ") + "${it.key}**" }
            .let { str ->
                if (tierlist.mode.isTiersWithFree()) str + "; ${tierlist.freePicksAmount - picks[idx]!!.count { it.free }}x **Free Pick**"
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


    fun DraftData.getTierInsertIndex(takePicks: Int = picks.size): Int {
        var index = 0
        val picksToUse = this.picks.take(takePicks)
        for (entry in tierlist.prices.entries) {
            if (entry.key == this.tier) {
                return picksToUse.count { !it.free && !it.quit && it.tier == this.tier } + index - 1
            }
            index += entry.value
        }
        error("Tier ${this.tier} not found by user $current")
    }

    fun addToMoved(data: NextPlayerData.Moved) {
        if (!isSwitchDraft) moved.getOrPut(current) { mutableListOf() }.let { if (round !in it) it += round }
    }

    private fun addPunishableSkippedRound(data: NextPlayerData.Moved) {
        if (System.currentTimeMillis() > (timer?.getCurrentTimerInfo()?.startPunishSkipsTime
                ?: 0) && (data.reason == SkipReason.REALTIMER || data.skippedBy != null)
        ) punishableSkippedTurns.getOrPut(current) { mutableSetOf() } += round
    }

    fun hasMovedTurns(idx: Int = current) = movedTurns(idx).isNotEmpty()
    fun movedTurns(idx: Int = current) = moved[idx] ?: mutableListOf()

    private suspend fun endOfTurn(): Boolean {
        logger.debug("End of turn")
        if (order[round]!!.isEmpty()) {
            logger.debug("No more players")
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
        isRunning = false
        save("END SAVE")
    }

    internal open suspend fun getCurrentMention(): String {
        val currentId = this[current]
        val data = allowed[current] ?: return "<@$currentId>"
        val currentData = data.firstOrNull { it.u == currentId } ?: AllowedData(currentId, true)
        val (teammates, other) = data.filter { it.mention && it.u != currentId }.partition { it.teammate }
        return (if (currentData.mention) "<@$currentId>" else "**${getCurrentName()}**") + teammates.joinToString { "<@${it.u}>" }
            .ifNotEmpty { ", $it" } + other.joinToString { "<@${it.u}>" }.ifNotEmpty { ", ||$it||" }
    }

    fun getCurrentName(idx: Int = current) = names[idx]

    fun indexInRound(round: Int): Int = originalorder[round]!!.indexOf(current)

    fun cancelCurrentTimer(reason: String = "Next player") {
        cooldownJob?.cancel(reason)
        stallSecondJob?.cancel(reason)
    }


    open suspend fun onAfterPick(data: NextPlayerData) {}

    fun addFinished(idx: Int) {
        order.values.forEach { it.remove(idx) }
    }

    @Transient
    open val docEntry: DocEntry? = null

    @Transient
    open val dataSheet: String = "Data"

    fun builder() = RequestBuilder(sid)

    context (InteractionData)
    suspend fun replyGeneral(
        msg: String,
        components: Collection<LayoutComponent> = SendDefaults.components,
        ifTestUseTc: MessageChannel? = null
    ) = replyWithTestInteractionCheck(
        "<@${user}> hat${
            if (user != this[current]) " für **${getCurrentName()}**" else ""
        } $msg", components, ifTestUseTc
    )


    context(InteractionData)
    suspend fun replyWithTestInteractionCheck(
        content: String,
        components: Collection<LayoutComponent> = SendDefaults.components,
        ifTestUseTc: MessageChannel? = null
    ) = ifTestUseTc?.takeIf { self is TestInteractionData }?.send(content, components = components)?.await()
        ?: replyAwait(
            content, components = components
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
        description = "Spieltag ${gdData.gameday}: " + league.uindices.joinToString(" vs. ") { "<@${table[it]}>" }
    }

    open suspend fun onReplayAnalyse(data: ReplayData) {}

    fun getGameplayData(idx1: Int, idx2: Int, game: List<DraftPlayer>): GamedayData {
        var u1IsSecond = false
        val gameday = battleorder.asIterable().reversed().firstNotNullOfOrNull {
            if (it.value.any { l ->
                    l.containsAll(listOf(idx1, idx2)).also { b ->
                        if (b) u1IsSecond = l.indexOf(idx1) == 1
                    }
                }) it.key else null
        } ?: -1
        val numbers = (0..1).reversedIf(u1IsSecond).map { game[it].alivePokemon }
        val battleindex = battleorder[gameday]?.let { battleorder ->
            (battleorder.indices.firstOrNull { battleorder[it].contains(idx1) } ?: -1)
        } ?: -1

        return GamedayData(
            gameday, battleindex, u1IsSecond, numbers
        )
    }

    fun storeMatch(replayData: ReplayData) {
        val data = replayDataStore?.data ?: return
        data.getOrPut(replayData.gamedayData.gameday) { mutableMapOf() }[replayData.gamedayData.battleindex] =
            replayData
    }

    fun getMatchups(gameday: Int) = getMatchupsIndices(gameday).map { mu -> mu.map { table[it] } }
    fun getMatchupsIndices(gameday: Int) = battleorder[gameday]!!

    open fun executeTipGameSending(num: Int) {
        launch {
            val tip = tipgame!!
            var shouldSave = false
            for (gameday in tip.tips.keys.toList()) {
                if (gameday >= num) {
                    tip.tips.remove(gameday)
                    shouldSave = true
                }
            }
            if (shouldSave) save("TipGameSending ResetOfTipGame")
            val channel = jda.getTextChannelById(tip.channel)!!
            val matchups = getMatchups(num)
            val names = jda.getGuildById(guild)!!.retrieveMembersByIds(matchups.flatten()).await()
                .associate { it.idLong to it.effectiveName }
            channel.send(
                embeds = Embed(
                    title = "Spieltag $num",
                    color = Color.YELLOW.rgb,
                    description = if (tip.withCurrentState) "Bisherige Votes: 0:0" else null
                ).into()
            ).queue()
            for ((index, matchup) in matchups.withIndex()) {
                val u1 = matchup[0]
                val u2 = matchup[1]
                val base: ArgBuilder<TipGameManager.VoteButton.Args> = {
                    this.leaguename = this@League.leaguename
                    this.gameday = num
                    this.index = index
                }
                channel.send(
                    embeds = Embed(
                        title = "${names[u1]} vs. ${names[u2]}", color = embedColor
                    ).into(), components = ActionRow.of(TipGameManager.VoteButton(names[u1]!!) {
                        base()
                        this.userindex = u1.indexedBy(table)
                    }, TipGameManager.VoteButton(names[u2]!!) {
                        base()
                        this.userindex = u2.indexedBy(table)
                    }).into()
                ).queue()
            }
        }
    }

    fun executeTipGameLockButtons() {
        launch {
            jda.getTextChannelById(tipgame!!.channel)!!.iterableHistory.takeAsync(battleorder.entries.first().value.size)
                .await().forEach {
                    it.editMessageComponents(
                        ActionRow.of(it.actionRows[0].buttons.map { button -> button.asDisabled() })
                    ).queue()
                    delay(2000)
                }
        }
    }

    fun executeTipGameLockButtonsIndividual(gameday: Int, mu: Int) {
        launch {
            val muCount = battleorder[gameday]!!.size
            jda.getTextChannelById(tipgame!!.channel)!!.iterableHistory.takeAsync(muCount + 1).await().let {
                it.dropWhile { m -> !m.author.isBot }[muCount - mu - 1]
            }.let {
                it.editMessageComponents(
                    ActionRow.of(it.actionRows[0].buttons.map { b -> b.asDisabled() })
                ).queue()
            }
        }
    }

    open suspend fun executeYoutubeSend(
        ytTC: Long, gameday: Int, battle: Int, strategy: VideoProvideStrategy, overrideEnabled: Boolean = false
    ) {
    }

    fun buildStoreStatus(gameday: Int): String {
        val dataStore = replayDataStore ?: error("No replay data store found")
        val gamedayData = dataStore.data[gameday].orEmpty()
        return "## Aktueller Stand von Spieltag $gameday:\n" + (0..<battleorder[1]!!.size).joinToString("\n") {
            "${battleorder[gameday]!![it].joinToString(" vs. ") { u -> "<@${table[u]}>" }}: ${if (gamedayData[it] != null) "✅" else "❌"}"
        }

    }

    inner class PointsManager {
        private val points = mutableMapOf<Int, Int>()

        operator fun get(idx: Int) = points.getOrPut(idx) {
            val isPoints = tierlist.mode.isPoints()
            tierlist.points - picks[idx].orEmpty().filterNot { it.quit }.sumOf {
                if (it.free) tierlist.freepicks[it.tier]!! else if (isPoints) tierlist.prices.getValue(
                    it.tier
                ) else if (tierlist.variableMegaPrice && it.name.isMega) it.tier.substringAfter("#").toInt() else 0
            }
        }

        fun add(idx: Int, points: Int) {
            this.points[idx] = this[idx] + points
        }
    }

    val timeFormat get() = if (timer?.stallSeconds == 0) leagueTimeFormat else leagueTimeFormatSecs

    companion object : CoroutineScope {
        override val coroutineContext = createCoroutineContext("League", Dispatchers.IO)
        val logger = KotlinLogging.logger {}
        val allTimers = mutableMapOf<String, Job>()
        val allStallSecondTimers = mutableMapOf<String, Job>()
        val leagueTimeFormat = SimpleDateFormat("HH:mm")
        val leagueTimeFormatSecs = SimpleDateFormat("HH:mm:ss")
        private val allMutexes = ConcurrentHashMap<String, Mutex>()
        val queueInteractionData = TestInteractionData(tc = 1099651412742389820)
        val timerScope = createCoroutineScope("LeagueTimer")

        fun getLock(leaguename: String): Mutex = allMutexes.getOrPut(leaguename) { Mutex() }

        suspend inline fun executeOnFreshLock(
            leagueSupplier: () -> League?, onNotFound: () -> Unit = {}, block: League.() -> Unit
        ) {
            executeOnFreshLock(leagueSupplier, League::leaguename, onNotFound, block)
        }

        suspend inline fun <T> executeOnFreshLock(
            supplier: () -> T?, leagueNameMapper: (T) -> String, onNotFound: () -> Unit = {}, block: T.() -> Unit
        ) {
            val league = supplier() ?: return onNotFound()
            val lock = getLock(leagueNameMapper(league))
            val wasLocked = lock.isLocked
            lock.withLock {
                (if (wasLocked) supplier()!! else league).apply(block)
            }
        }

        suspend inline fun executeOnFreshLock(name: String, block: League.() -> Unit) = getLock(name).withLock {
            val league = db.getLeague(name) ?: return@withLock logger.error("ExecuteOnFreshLock failed for $name")
            league.block()
        }

        suspend fun executeTimerOnRefreshedVersion(name: String) {
            executeOnFreshLock(name) {
                if (timerRelated.cooldown <= System.currentTimeMillis()) afterPickOfficial(
                    data = NextPlayerData.Moved(
                        SkipReason.REALTIMER, current
                    )
                )
            }
        }

        context(InteractionData)
        suspend inline fun executePickLike(block: League.() -> Unit) {
            executeOnFreshLock({ byCommand() }, { it.first.leaguename }, {
                if (!replied) {
                    reply(
                        "Es läuft zurzeit kein Draft in diesem Channel!", ephemeral = true
                    )
                }
            }) {
                // this is only needed when timerSkipMode is AFTER_DRAFT_UNORDERED
                if (first.pseudoEnd && first.afterTimerSkipMode == AFTER_DRAFT_UNORDERED) {
                    // BypassCurrentPlayerData can only be Yes here
                    first.current = (second as BypassCurrentPlayerData.Yes).user
                }
                if (!first.isCurrentCheck(user)) {
                    return@executeOnFreshLock reply("Du warst etwas zu langsam!", ephemeral = true)
                }
                first.block()
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
        context(InteractionData)
        suspend inline fun executeAsNotCurrent(asParticipant: Boolean, block: League.() -> Unit) {
            executeOnFreshLock({ onlyChannel(tc)?.takeIf { !asParticipant || user in it.table } }, {
                reply(
                    if (asParticipant) "In diesem Channel läuft kein Draft, an welchem du teilnimmst!" else "Es läuft zurzeit kein Draft in diesem Channel!",
                    ephemeral = true
                )
            }, block)

        }
    }
}

@Serializable
sealed interface LeagueConfig {
    companion object {
        val defaultConfigs = mutableMapOf<KClass<*>, LeagueConfig>()
        inline fun <reified T : LeagueConfig> getDefaultConfig(): T {
            return defaultConfigs.getOrPut(T::class) { T::class.primaryConstructor!!.callBy(emptyMap()) } as T
        }
    }
}

@Serializable
@SerialName("AllowPickDuringSwitch")
data object AllowPickDuringSwitch : LeagueConfig

@Serializable
@SerialName("DraftBanConfig")
data class DraftBanConfig(
    val banRounds: Map<Int, BanRoundConfig> = mapOf(),
    val notBannable: Set<String> = setOf(),
    val skipBehavior: BanSkipBehavior = BanSkipBehavior.NOTHING
) : LeagueConfig

@Serializable
@SerialName("RandomPick")
data class RandomPickConfig(
    val disabled: Boolean = false,
    val mode: RandomPickMode = RandomPickMode.Default(),
    val jokers: Int = 0,
    val onlyOneMega: Boolean = false,
    val tierRestrictions: Set<String> = emptySet()
) : LeagueConfig {
    fun hasJokers() = jokers > 0
}

data class RandomPickUserInput(
    val tier: String?,
    val type: String?,
    val ignoreRestrictions: Boolean = false,
    val skipMons: Set<String> = emptySet()
)

@Serializable
sealed interface RandomPickMode {
    context(InteractionData)
    suspend fun League.getRandomPick(input: RandomPickUserInput, config: RandomPickConfig): Pair<DraftName, String>?

    /**
     * @return a map of the possible command options for the randompick command [true = required, false = optional, null = not available]
     */
    fun provideCommandOptions(): Map<RandomPickArgument, Boolean>

    @Serializable
    @SerialName("Default")
    data class Default(val tierRequired: Boolean = false, val typeAllowed: Boolean = true) : RandomPickMode {
        override fun provideCommandOptions(): Map<RandomPickArgument, Boolean> {
            return buildMap {
                put(RandomPickArgument.TIER, tierRequired)
                if (typeAllowed) put(RandomPickArgument.TYPE, false)
            }
        }

        context(InteractionData) override suspend fun League.getRandomPick(
            input: RandomPickUserInput, config: RandomPickConfig
        ): Pair<DraftName, String>? {
            if (tierRequired && input.tier == null) return replyNull("Du musst ein Tier angeben!")
            val tier = if (input.ignoreRestrictions) input.tier!! else parseTier(input.tier, config) ?: return null
            val list = tierlist.getByTier(tier)!!.shuffled()
            val skipMega = config.onlyOneMega && picks[current]!!.any { it.name.isMega }
            return firstAvailableMon(list) { german, english ->
                if (german in input.skipMons || (skipMega && english.isMega)) return@firstAvailableMon false
                if (input.type != null) input.type in db.pokedex.get(english.toSDName())!!.types else true
            }?.let { it to tier }
                ?: return replyNull("In diesem Tier gibt es kein Pokemon mit dem angegebenen Typen mehr!")
        }
    }

    // Only compatible with TierlistMode.TIERS
    @Serializable
    @SerialName("TypeTierlist")
    data object TypeTierlist : RandomPickMode {
        private val logger = KotlinLogging.logger {}
        override fun provideCommandOptions(): Map<RandomPickArgument, Boolean> {
            return mapOf(RandomPickArgument.TYPE to true)
        }

        context(InteractionData) override suspend fun League.getRandomPick(
            input: RandomPickUserInput, config: RandomPickConfig
        ): Pair<DraftName, String>? {
            val type = input.type ?: return replyNull("Du musst einen Typen angeben!")
            val picks = picks[current]!!
            var mon: DraftName? = null
            var tier: String? = null
            val usedTiers = mutableSetOf<String>()
            val skipMega = config.onlyOneMega && picks.any { it.name.isMega }
            val prices = tierlist.prices
            for (i in 0..prices.size) {
                val temptier =
                    prices.filter { (tier, amount) -> tier !in usedTiers && picks.count { mon -> mon.tier == tier } < amount }.keys.randomOrNull()
                        ?: return replyNull("Es gibt kein $type-Pokemon mehr, welches in deinen Kader passt!")
                val tempmon = firstAvailableMon(
                    tierlist.getWithTierAndType(temptier, type).shuffled()
                ) { german, _ -> german in input.skipMons && !(german.isMega && skipMega) }
                if (tempmon != null) {
                    mon = tempmon
                    tier = temptier
                    break
                }
                usedTiers += temptier
            }
            if (mon == null) {
                logger.error("No pokemon found without error message: $current $type")
                return replyNull("Es ist konnte kein passendes Pokemon gefunden werden! (<@${Constants.FLOID}>)")
            }
            return mon to tier!!
        }
    }

    context(League, InteractionData)
    fun parseTier(tier: String?, config: RandomPickConfig): String? {
        if (tier == null) return if (tierlist.mode.withTiers) getPossibleTiers().filter { it.value > 0 }.keys.random() else tierlist.order.last()
        val parsedTier = tierlist.order.firstOrNull { it.equals(tier, ignoreCase = true) }
        if (parsedTier == null) {
            return replyNull("Das Tier `$tier` existiert nicht!")
        }
        if (config.tierRestrictions.isNotEmpty() && parsedTier !in config.tierRestrictions) {
            return replyNull("In dieser Liga darf nur in folgenden Tiers gerandompickt werden: ${config.tierRestrictions.joinToString()}")
        }
        if (handleTiers(parsedTier, parsedTier)) return null
        if (handlePoints(false, parsedTier)) return null
        return parsedTier
    }
}

@Serializable
data class RandomLeagueData(
    var currentMon: RandomLeaguePick? = null, val jokers: MutableMap<Int, Int> = mutableMapOf()
)

@Serializable
data class RandomLeaguePick(
    val official: String,
    val tlName: String,
    val tier: String,
    val data: Map<String, String?> = mapOf(),
    val history: Set<String> = setOf(),
    var disabled: Boolean = false
)


enum class RandomPickArgument {
    TIER, TYPE
}

@Serializable
@SerialName("TeraAndZ")
data class TeraAndZ(val z: TZDataHolder? = null, val tera: TeraData? = null) : LeagueConfig

@Serializable
data class TZDataHolder(
    val coord: DynamicCoord, val searchRange: String, val searchColumn: Int, val firstTierAllowed: String? = null
)

@Serializable
data class TeraData(val type: TZDataHolder, val mon: TZDataHolder)

@Serializable
data class QueuePicksData(
    @EncodeDefault var enabled: Boolean = false,
    var disableIfSniped: Boolean = true,
    @EncodeDefault var queued: MutableList<QueuedAction> = mutableListOf()
)

@Serializable
data class ReplayDataStore(
    val data: MutableMap<Int, MutableMap<Int, ReplayData>> = mutableMapOf(),
    @Serializable(with = InstantToStringSerializer::class) val lastUploadStart: Instant,
    val lastReminder: Reminder? = null,
    @Serializable(with = DurationSerializer::class) val intervalBetweenUploadAndVideo: Duration = Duration.ZERO,
    @Serializable(with = DurationSerializer::class) val intervalBetweenGD: Duration,
    @Serializable(with = DurationSerializer::class) val intervalBetweenMatches: Duration,
    val amount: Int,
)

@Serializable
data class Reminder(@Serializable(with = InstantToStringSerializer::class) val lastSend: Instant, val channel: Long)

@Serializable
data class TimerRelated(
    var cooldown: Long = -1,
    var regularCooldown: Long = -1,
    @EncodeDefault var lastPick: Long = -1,
    var lastRegularDelay: Long = -1,
    @EncodeDefault val usedStallSeconds: MutableMap<Int, Int> = mutableMapOf(),
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
    open val idx: Int,
    open val round: Int,
) {
    val roundIndex get() = round - 1
    val indexInRound get() = league.indexInRound(round)
    val changedIndex get() = picks.indexOfFirst { it.name == pokemonofficial }
    val picks by lazy { league.picks[idx]!! }
    abstract val changedOnTeamsiteIndex: Int

    val displayName = OneTimeCache {
        "$pokemon/${
            NameConventionsDB.getSDTranslation(
                pokemonofficial, league.guild, english = true
            )!!.tlName
        }"
    }
}


data class PickData(
    override val league: League,
    override val pokemon: String,
    override val pokemonofficial: String,
    override val tier: String,
    override val idx: Int,
    override val round: Int,
    val freePick: Boolean,
    val updrafted: Boolean
) : DraftData(league, pokemon, pokemonofficial, tier, idx, round) {
    override val changedOnTeamsiteIndex: Int by lazy {
        with(league) { getTierInsertIndex() }
    }
}


class SwitchData(
    league: League,
    pokemon: String,
    pokemonofficial: String,
    tier: String,
    mem: Int,
    round: Int,
    val oldmon: DraftName,
    val oldIndex: Int
) : DraftData(league, pokemon, pokemonofficial, tier, mem, round) {
    override val changedOnTeamsiteIndex by lazy { with(league) { getTierInsertIndex(oldIndex + 1) } }

    val oldDisplayName = OneTimeCache {
        "${oldmon.tlName}/${
            NameConventionsDB.getSDTranslation(
                oldmon.official, league.guild, english = true
            )!!.tlName
        }"
    }
}

class BanData(
    league: League,
    pokemon: String,
    pokemonofficial: String,
    tier: String,
    mem: Int,
    round: Int,
) : DraftData(league, pokemon, pokemonofficial, tier, mem, round) {
    override val changedOnTeamsiteIndex = -1 // not used for BanData
}

data class TierData(val specified: String, val official: String)

sealed interface TimerSkipMode {
    suspend fun League.afterPickCall(data: NextPlayerData): TimerSkipResult

    /**
     * What happens after a pick/timer skip
     * @param data the data of the pick
     * @return if the next player should be announced
     */
    suspend fun League.afterPick(data: NextPlayerData): TimerSkipResult
    suspend fun League.getPickRound(): Int

    suspend fun League.bypassCurrentPlayerCheck(user: Long): BypassCurrentPlayerData = BypassCurrentPlayerData.No
    fun League.disableTimer() = false
}

sealed interface BypassCurrentPlayerData {
    data object No : BypassCurrentPlayerData
    data class Yes(val user: Int) : BypassCurrentPlayerData
}

interface DuringTimerSkipMode : TimerSkipMode {
    override suspend fun League.afterPickCall(data: NextPlayerData) = afterPick(data)
}

interface AfterTimerSkipMode : TimerSkipMode {
    override suspend fun League.afterPickCall(data: NextPlayerData) =
        if (draftWouldEnd) afterPick(data) else TimerSkipResult.NEXT
}

@Serializable
data object LAST_ROUND : DuringTimerSkipMode {
    override suspend fun League.afterPick(data: NextPlayerData): TimerSkipResult {
        if (isLastRound && hasMovedTurns()) {
            return TimerSkipResult.SAME
        }
        return TimerSkipResult.NEXT
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
            TimerSkipResult.SAME
        } else TimerSkipResult.NEXT


    override suspend fun League.getPickRound(): Int = movedTurns().firstOrNull() ?: round
}

@Serializable
data object AFTER_DRAFT_ORDERED : AfterTimerSkipMode {
    override suspend fun League.afterPick(data: NextPlayerData): TimerSkipResult {
        populateAfterDraft()
        return TimerSkipResult.NEXT
    }


    override suspend fun League.getPickRound() = if (pseudoEnd) movedTurns().removeFirst()
    else round

    private fun League.populateAfterDraft() {
        val order = order[totalRounds]!!
        for (i in 1..totalRounds) {
            moved.forEach { (user, turns) -> if (i in turns) order.add(user.indexedBy(table)) }
        }
        if (order.isNotEmpty()) {
            pseudoEnd = true
        }
    }
}

@Serializable
data object AFTER_DRAFT_UNORDERED : AfterTimerSkipMode {
    override suspend fun League.afterPick(data: NextPlayerData): TimerSkipResult =
        if (moved.values.any { it.isNotEmpty() }) {
            if (!pseudoEnd) {
                tc.sendMessage("Der Draft wäre jetzt vorbei, aber es gibt noch Spieler, die keinen vollständigen Kader haben! Diese können nun in beliebiger Reihenfolge ihre Picks nachholen. Dies sind:\n" + moved.entries.filter { it.value.isNotEmpty() }
                    .joinToString("\n") { (user, turns) -> "<@${table[user]}>: ${turns.size}${announceData(false)}" })
                    .queue()
                cancelCurrentTimer()
                pseudoEnd = true
            }
            TimerSkipResult.PSEUDOEND
        } else TimerSkipResult.NEXT


    override suspend fun League.getPickRound(): Int = if (pseudoEnd) {
        movedTurns().removeFirst()
    } else round


    override suspend fun League.bypassCurrentPlayerCheck(user: Long): BypassCurrentPlayerData {
        val no = BypassCurrentPlayerData.No
        val idx = this(user)
        // Are we in the pseudo end?
        if (!pseudoEnd) return no
        // Has the user moved turns?
        if (hasMovedTurns(idx)) return BypassCurrentPlayerData.Yes(idx)
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
    data class Moved(val reason: SkipReason, val skippedUser: Int, val skippedBy: Long? = null) : NextPlayerData
}

enum class SkipReason {
    REALTIMER, SKIP
}

enum class TimerSkipResult {
    NEXT, SAME, PSEUDOEND
}

@Serializable
data class GamedayData(
    val gameday: Int, val battleindex: Int, val u1IsSecond: Boolean, var numbers: List<Int> = emptyList()
)

sealed interface VideoProvideStrategy {
    suspend fun League.provideVideoId(index: Int, uindex: Int): String?

    data object Fetch : VideoProvideStrategy {
        override suspend fun League.provideVideoId(index: Int, uindex: Int): String? {
            return Google.fetchLatestVideosFromChannel(db.ytchannel.get(table[uindex])!!.channelId).filter { lastVid ->
                (System.currentTimeMillis() - lastVid.snippet.publishedAt.value) <= 1000 * 60 * 60 * 4
            }.let { vids ->
                League.logger.info(vids.map { it.snippet.title }.toString())
                vids.singleOrNull() ?: vids.firstOrNull { it.snippet.title.contains("IPL", ignoreCase = true) }
            }?.id?.videoId
        }
    }

    data class Subscribe(private val ytData: YTVideoSaveData) : VideoProvideStrategy {
        override suspend fun League.provideVideoId(index: Int, uindex: Int): String? {
            return ytData.vids[index]
        }
    }

}

@Serializable
sealed interface BanRoundConfig {

    fun checkBan(tier: String, alreadyBanned: Set<DraftPokemon>): String?
    fun getPossibleBanTiers(alreadyBanned: Set<DraftPokemon>): List<String>


    @Serializable
    @SerialName("FixedTier")
    data class FixedTier(val tier: String) : BanRoundConfig {
        override fun checkBan(tier: String, alreadyBanned: Set<DraftPokemon>) =
            if (this.tier != tier) "In dieser Runde können nur Pokemon aus dem `${this.tier}`-Tier gebannt werden!" else null

        override fun getPossibleBanTiers(alreadyBanned: Set<DraftPokemon>) = listOf(tier)
    }

    @Serializable
    @SerialName("FixedTierSet")
    data class FixedTierSet(val tierSet: Map<String, Int>) : BanRoundConfig {
        override fun checkBan(
            tier: String, alreadyBanned: Set<DraftPokemon>
        ): String? {
            val originalBanAmount = tierSet[tier] ?: return "Man kann keine Pokemon aus dem `$tier`-Tier bannen!"
            val alreadyBannedAmount = alreadyBanned.count { it.tier == tier }
            return if (originalBanAmount - alreadyBannedAmount <= 0) "Du kannst aus dem $tier-Tier keine Pokemon mehr bannen!" else null
        }

        override fun getPossibleBanTiers(alreadyBanned: Set<DraftPokemon>): List<String> {
            val alreadyBanned = alreadyBanned.groupBy { it.tier }
            return tierSet.entries.filter { it.value - (alreadyBanned[it.key]?.size ?: 0) > 0 }.map { it.key }
        }
    }
}


enum class BanSkipBehavior {
    NOTHING, RANDOM
}