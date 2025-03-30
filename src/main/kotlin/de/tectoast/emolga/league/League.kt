@file:OptIn(ExperimentalSerializationApi::class)

package de.tectoast.emolga.league

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.exposed.DraftName
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.database.exposed.TipGameMessagesDB
import de.tectoast.emolga.features.ArgBuilder
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.TestInteractionData
import de.tectoast.emolga.features.draft.AddToTierlistData
import de.tectoast.emolga.features.draft.TipGameManager
import de.tectoast.emolga.features.flo.SendFeatures
import de.tectoast.emolga.league.config.LeagueConfig
import de.tectoast.emolga.league.config.PersistentLeagueData
import de.tectoast.emolga.league.config.ResettableLeagueData
import de.tectoast.emolga.league.config.TimerRelated
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.draft.*
import de.tectoast.emolga.utils.draft.DraftUtils.executeWithinLock
import de.tectoast.emolga.utils.json.LeagueResult
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.json.only
import de.tectoast.emolga.utils.showdown.AnalysisData
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
import org.litote.kmongo.ne
import java.text.SimpleDateFormat
import java.util.concurrent.ConcurrentHashMap
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.math.min
import kotlin.time.measureTime

enum class DraftState {
    OFF, ON, PSEUDOEND
}

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
    var draftState: DraftState = DraftState.OFF
    val isRunning: Boolean get() = draftState != DraftState.OFF
    val pseudoEnd: Boolean get() = draftState == DraftState.PSEUDOEND
    val picks: MutableMap<Int, MutableList<DraftPokemon>> = mutableMapOf()
    val battleorder: MutableMap<Int, List<List<Int>>> = mutableMapOf()
    val allowed: MutableMap<Int, MutableSet<AllowedData>> = mutableMapOf()
    val guild = -1L

    @EncodeDefault
    var round = 1
    val current get() = currentOverride ?: order[round]!![0]

    @Transient
    var currentOverride: Int? = null

    @Transient
    var shouldSave = false

    abstract val teamsize: Int
    open val gamedays: Int by lazy { battleorder.let { if (it.isEmpty()) table.size - 1 else it.size } }

    @Transient
    val points: PointsManager = PointsManager()

    @Transient
    open val afterTimerSkipMode: AfterTimerSkipMode = AFTER_DRAFT_UNORDERED

    @Transient
    open val duringTimerSkipMode: DuringTimerSkipMode? = null

    val originalorder: Map<Int, List<Int>> = mapOf()

    val order: MutableMap<Int, MutableList<Int>> = mutableMapOf()

    @EncodeDefault
    val moved: MutableMap<Int, MutableList<Int>> = mutableMapOf()

    @EncodeDefault
    val punishableSkippedTurns: MutableMap<Int, MutableSet<Int>> = mutableMapOf()
    val names: MutableList<String> = mutableListOf()


    val tc: TextChannel get() = jda.getTextChannelById(tcid) ?: error("No text channel found for guild $guild")
    val currentTimerSkipMode: TimerSkipMode
        get() = duringTimerSkipMode?.takeUnless { draftWouldEnd } ?: afterTimerSkipMode

    @Contextual
    var tcid: Long = -1


    @Transient
    private var tlCompanion = Tierlist // workaround

    val tierlist: Tierlist by tlCompanion

    @Transient
    open val pickBuffer = 0

    @Transient
    var newTimerForAnnounce = false


    val cooldownJob: Job? get() = allTimers[leaguename]
    val stallSecondJob: Job? get() = allStallSecondTimers[leaguename]

    @Transient
    var lastPickedMon: DraftName? = null

    val isLastRound: Boolean get() = round == totalRounds
    val totalRounds by lazy { originalorder.size }

    var isSwitchDraft = false

    val table: List<@Contextual Long> = ArrayList()

    val tierorderingComparator by lazy { compareBy<DraftPokemon>({ tierlist.order.indexOf(it.tier) }, { it.name }) }


    val newSystemGap get() = teamsize + pickBuffer + 3

    open val additionalSet: AdditionalSet? by lazy { AdditionalSet((gamedays + 4).xc(), "X", "Y") }

    val config: LeagueConfig = LeagueConfig()

    @EncodeDefault
    var draftData: ResettableLeagueData = ResettableLeagueData()
    val persistentData: PersistentLeagueData = PersistentLeagueData()

    val draftWouldEnd get() = isLastRound && order[round]!!.size <= 1

    @Transient
    open val docEntry: DocEntry? = null

    @Transient
    open val dataSheet: String = "Data"


    operator fun invoke(mem: Long) = table.indexOf(mem)
    operator fun get(index: Int) = table[index]

    suspend fun currentOrFromID(id: Long) = currentOverride ?: order[round]?.get(0)
    ?: with(afterTimerSkipMode) { bypassCurrentPlayerCheck(id) as? BypassCurrentPlayerData.Yes }?.user

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

    open fun PickData.savePick(noCost: Boolean = false) {
        picks.add(DraftPokemon(pokemonofficial, tier, freePick, noCost = noCost))
    }

    open fun SwitchData.saveSwitch() {
        picks.first { it.name == oldmon.official }.quit = true
        picks += DraftPokemon(pokemonofficial, tier)
    }

    open fun BanData.saveBan() {
        draftData.draftBan.bannedMons.getOrPut(round) { mutableSetOf() }.add(DraftPokemon(pokemonofficial, tier))
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
        (picks.values + draftData.draftBan.bannedMons.values).any { l ->
            l.any {
                !it.quit && it.name.equals(
                    mon, ignoreCase = true
                )
            }
        }

    suspend inline fun firstAvailableMon(
        tlNames: Collection<String>,
        checker: (DraftName.(german: String, english: String) -> Boolean) = { _, _ -> true }
    ): DraftName? {
        val alreadyPicked =
            (picks.values + draftData.draftBan.bannedMons.values).flatten().mapTo(mutableSetOf()) { it.name }
        return tlNames.firstNotNullOfOrNull {
            val draftName = NameConventionsDB.getDiscordTranslation(
                it, guild, tierlist.isEnglish
            )!!
            if (draftName.official !in alreadyPicked && draftName.checker(
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
        free: Boolean, tier: String, tierOld: String? = null, mega: Boolean = false, extraCosts: Int? = null
    ): Boolean {
        if (!tierlist.mode.withPoints) return false
        if (tierlist.mode.isTiersWithFree() && !(tierlist.variableMegaPrice && mega) && !free && extraCosts == null) return false
        val hasTeraPick = config.teraPick != null
        val alreadyPaid = draftData.teraPick.alreadyPaid
        if (hasTeraPick && extraCosts != null && current in alreadyPaid) {
            reply("Du hast bereits einen Tera-Pick!")
            return true
        }
        if (hasTeraPick && isLastRound && current !in alreadyPaid && extraCosts == null) {
            reply("Du musst noch einen Tera-Pick machen!")
            return true
        }
        val cpicks = picks[current]!!
        val currentPicksHasMega by lazy { cpicks.any { it.name.isMega } }
        if (tierlist.variableMegaPrice && currentPicksHasMega && mega) {
            reply("Du kannst nur ein Mega draften!")
            return true
        }
        val variableMegaPrice = (if (tierlist.variableMegaPrice) (if (!currentPicksHasMega) tierlist.order.mapNotNull {
            it.substringAfter("#", "").takeUnless { t -> t.isEmpty() }?.toInt()
        }.minOrNull() ?: 0 else 0) else 0).let {
            if (mega) 0 else it
        }
        val needed = (if (free) tierlist.getPointsNeeded(tier) else 0) + (extraCosts ?: 0) + variableMegaPrice
        val pointsBack = tierOld?.let { tierlist.getPointsNeeded(it) } ?: 0
        val newPoints = points[current] - needed + pointsBack
        if (newPoints < 0) {
            reply("Dafür hast du nicht genug Punkte! (`${points[current]} - $needed${if (pointsBack == 0) "" else "+ $pointsBack"} = $newPoints < 0`)")
            return true
        }
        if (pointsBack == 0) {
            val minimumRequired = when (tierlist.mode) {
                TierlistMode.POINTS -> {
                    minimumNeededPointsForTeamCompletion(cpicks.count { !it.noCost } + 1)
                }

                TierlistMode.TIERS_WITH_FREE -> {
                    val amountLeft = tierlist.freePicksAmount - (cpicks.count { it.free } + (if (free) 1 else 0))
                    val minimumPrice = tierlist.freepicks.entries.filter { it.key != "#AMOUNT#" && "Mega#" !in it.key }
                        .minOf { it.value }

                    amountLeft * minimumPrice
                }

                else -> {
                    0
                }
            }
            if (newPoints < minimumRequired) {
                reply("Wenn du dir dieses Pokemon holen würdest, kann dein Kader nicht mehr vervollständigt werden! (Du musst nach diesem Pick noch mindestens $minimumRequired Punkte haben, hättest aber nur noch $newPoints)")
                return true
            }
        }
        points[current] = newPoints
        return false
    }

    fun minimumNeededPointsForTeamCompletion(picksSizeAfterAdd: Int) =
        (min(totalRounds, tierlist.maxMonsToPay) - picksSizeAfterAdd) * tierlist.prices.values.min()

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
        this.draftData.randomPick.currentMon?.disabled = true
        checkForQueuedPicksChanges()
        onAfterPick(data)
        if (data.isMoved()) {
            addPunishableSkippedRound(data)
            config.draftBan?.let { config ->
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
            addToMoved()
            data.sendSkipMessage()
        }
        val result: TimerSkipResult = run outer@{
            if (draftWouldEnd) {
                if (order[round]!!.size == 1 || afterTimerSkipMode == AFTER_DRAFT_ORDERED) {
                    duringTimerSkipMode?.run {
                        val duringResult = afterPick(data)
                        if (duringResult == TimerSkipResult.SAME) return@outer TimerSkipResult.SAME
                    }
                }
                afterTimerSkipMode.run {
                    afterPick(data)
                }
            } else {
                duringTimerSkipMode?.run { afterPick(data) } ?: TimerSkipResult.NEXT
            }
        }
        val ctm = System.currentTimeMillis()
        val timerRelated = this.draftData.timer
        timerRelated.lastPick = ctm
        timerRelated.lastStallSecondUsedMid = 0
        if (result != TimerSkipResult.SAME) {
            timerRelated.handleStallSecondPunishment(ctm)
            this@League.cancelCurrentTimer()
        }
        if (result == TimerSkipResult.NEXT) {
            nextUser()
            if (endOfTurn()) return
        }
        if (result == TimerSkipResult.NOCONCRETE) {
            nextUser()
            val randomOrder = moved.values.flatten().toSet().shuffled()
            for (idx in randomOrder) {
                if (tryQueuePick(idx)) break
            }
            save()
            return
        }

        if (tryQueuePick()) return


        if (result != TimerSkipResult.SAME) {
            restartTimer()
        }
        announcePlayer()
        save("NEXT PLAYER SAFE")
    }

    fun getAlreadyBannedMonsInThisRound(): Set<DraftPokemon> = draftData.draftBan.bannedMons[round].orEmpty()

    private fun TimerRelated.handleStallSecondPunishment(ctm: Long) {
        config.timer?.stallSeconds?.takeIf { it > 0 }?.let {
            if (cooldown > 0) {
                val punishSeconds = ((ctm - lastPick - lastRegularDelay) / 1000).toInt()
                if (punishSeconds > 0) usedStallSeconds.add(current, punishSeconds)
            }
        }
    }

    private suspend fun tryQueuePick(idx: Int = current): Boolean {
        config.draftBan?.banRounds[round]?.let { return false }
        config.randomPickRound?.takeIf { round in it.rounds }?.run {
            val randomMon = getRandomMon()
            with(queueInteractionData) outer@{
                executeWithinLock(
                    PickInput(pokemon = randomMon, tier = null, free = false, noCost = true),
                    type = DraftMessageType.QUEUE
                )
            }
            return true
        }
        val queuePicksData = persistentData.queuePicks.queuedPicks[idx]?.takeIf { it.enabled } ?: return false
        val queuedMon = queuePicksData.queued.firstOrNull() ?: return false
        with(queueInteractionData) outer@{
            executeWithinLock(queuedMon.buildDraftInput(), type = DraftMessageType.QUEUE)
        }
        return true
    }

    private fun checkForQueuedPicksChanges() {
        val newMon = lastPickedMon ?: return
        persistentData.queuePicks.queuedPicks.entries.filter { it.value.queued.any { mon -> mon.g == newMon } }
            .forEach { (mem, data) ->
                data.queued.removeIf { mon -> mon.g == newMon }
                if (mem != current) {
                    SendFeatures.sendToUser(
                        table[mem], embeds = Embed(
                            title = "Queue-Pick-Warnung",
                            color = 0xff0000,
                            description = "`${newMon.tlName}` aus deiner Queue wurde gepickt.\n${if (data.disableIfSniped) "Das System wurde für dich deaktiviert, damit du umplanen kannst." else "Das System läuft jedoch für dich weiter."}"
                        ).into()
                    )
                    data.enabled = data.enabled && !data.disableIfSniped
                }
            }
        lastPickedMon = null
    }


    suspend fun startDraft(
        tc: GuildMessageChannel?, fromFile: Boolean, switchDraft: Boolean?, nameGuildId: Long? = null
    ) {
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
        val currentTimeMillis = System.currentTimeMillis()
        if (!fromFile) {
            order.clear()
            order.putAll(originalorder.mapValues { it.value.toMutableList() })
            round = 1
            draftState = DraftState.ON
            moved.clear()
            draftData = ResettableLeagueData()
            punishableSkippedTurns.clear()
            reset()
            sendRound()
            if (tryQueuePick()) return
            restartTimer()
            announcePlayer()
            save("StartDraft")
        } else {
            val timerRelated = draftData.timer
            val delayData = if (timerRelated.cooldown > 0) DelayData(
                timerRelated.cooldown, timerRelated.regularCooldown, currentTimeMillis
            ) else config.timer?.calc(
                this, currentTimeMillis
            )
            restartTimer(delayData)
        }
        logger.info("Started!")
    }

    fun nextUser() {
        order[round]!!.removeFirstOrNull()
    }

    fun save(from: String = "") {
        shouldSave = true
    }

    override fun toString() = leaguename

    open fun reset() {}

    private fun restartTimer(delayData: DelayData? = config.timer?.calc(this)) {
        val skipDelay = delayData?.skipDelay
        with(draftData.timer) {
            skipDelay ?: run {
                cooldown = 0
                regularCooldown = 0
                return
            }
            afterTimerSkipMode.run {
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
            config.timer?.stallSeconds?.takeIf { it > 0 }?.let {
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

    protected open fun sendRound() {
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
        config.draftBan?.let { config ->
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
                ).condAppend(config.teraPick != null) {
                    " und Tera-Pick".let {
                        if (idx in draftData.teraPick.alreadyPaid) it.surroundWith("~~") else it
                    }
                }
            )
        }
    }.joinToString(prefix = " (", postfix = ")").let { if (it.length == 3) "" else it }
        .condAppend(withTimerAnnounce && newTimerForAnnounce) {
            " — Zeit bis: **${
                formatTimeFormatBasedOnDistance(draftData.timer.regularCooldown)
            }**"
        }.also { newTimerForAnnounce = false }

    open fun checkLegalDraftInput(input: DraftInput, type: DraftMessageType): String? {
        if (input is PickInput && type != DraftMessageType.RANDOM && config.randomPick.hasJokers()) return "In diesem Draft sind keine regulären Picks möglich!"
        config.draftBan?.let {
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

    fun addToMoved() {
        if (!isSwitchDraft) moved.getOrPut(current) { mutableListOf() }.let { if (round !in it) it += round }
    }

    private fun addPunishableSkippedRound(data: NextPlayerData.Moved) {
        if (System.currentTimeMillis() > (config.timer?.getCurrentTimerInfo()?.startPunishSkipsTime
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

    fun finishDraft(msg: String) {
        tc.sendMessage(msg).queue()
        draftState = DraftState.OFF
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
    suspend fun replySkip() {
        replyGeneral("den Pick übersprungen!")
    }

    suspend fun getPickRoundOfficial() = currentTimerSkipMode.run { getPickRound().also { save("GET PICK ROUND") } }

    open fun provideReplayChannel(jda: JDA): TextChannel? = null
    open fun provideResultChannel(jda: JDA): TextChannel? = null

    open fun appendedEmbed(data: AnalysisData, league: LeagueResult, gdData: GamedayData) = EmbedBuilder {
        val game = data.game
        val p1 = game[0].nickname
        val p2 = game[1].nickname
        title = "${data.ctx.format} replay: $p1 vs. $p2"
        url = data.ctx.url.takeIf { it.length > 10 } ?: "https://example.org"
        description =
            "Spieltag ${gdData.gameday.takeIf { it >= 0 } ?: "-"}: " + league.uindices.joinToString(" vs. ") { "<@${table[it]}>" }
    }

    open suspend fun onReplayAnalyse(data: ReplayData) {}

    fun getGamedayData(idx1: Int, idx2: Int, game: List<DraftPlayer>): GamedayData {
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
        persistentData.replayDataStore.data.getOrPut(replayData.gamedayData.gameday) { mutableMapOf() }[replayData.gamedayData.battleindex] =
            replayData
    }

    fun getMatchupsIndices(gameday: Int) = battleorder[gameday]!!

    open fun executeTipGameSending(num: Int, channelId: Long? = config.tipgame?.channel) {
        launch {
            val tip = config.tipgame ?: return@launch
            val channel = jda.getTextChannelById(channelId!!)!!
            val matchups = getMatchupsIndices(num)
            val names =
                jda.getGuildById(guild)!!.retrieveMembersByIds(table).await().sortedBy { it.idLong.indexedBy(table) }
                    .map { it.user.effectiveName }
            channel.send(
                embeds = Embed(
                    title = "Spieltag $num", color = tip.colorConfig.provideEmbedColor(this@League)
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
                val messageId = channel.send(
                    embeds = Embed(
                        title = "${names[u1]} vs. ${names[u2]}",
                        color = embedColor,
                        description = if (tip.withCurrentState) "Bisherige Votes: 0:0" else null
                    ).into(), components = ActionRow.of(TipGameManager.VoteButton(names[u1]) {
                        base()
                        this.userindex = u1
                    }, TipGameManager.VoteButton(names[u2]) {
                        base()
                        this.userindex = u2
                    }).into()
                ).await().idLong
                TipGameMessagesDB.set(leaguename, num, index, messageId)
            }
        }
    }

    fun executeTipGameLockButtons(gameday: Int) {
        launch {
            TipGameMessagesDB.get(leaguename, gameday).forEach {
                lockButtonsOnMessage(it)
                delay(2000)
            }
        }
    }


    fun executeTipGameLockButtonsIndividual(gameday: Int, mu: Int) {
        launch {
            TipGameMessagesDB.get(leaguename, gameday, mu).forEach {
                lockButtonsOnMessage(it)
            }
        }
    }

    private suspend fun lockButtonsOnMessage(
        messageId: Long
    ) {
        val tipgame = config.tipgame ?: return
        val tipGameChannel = jda.getTextChannelById(tipgame.channel)!!
        val message = tipGameChannel.retrieveMessageById(messageId).await()
        message.editMessageComponents(ActionRow.of(message.actionRows[0].buttons.map { button -> button.asDisabled() }))
            .queue()
    }

    open suspend fun executeYoutubeSend(
        ytTC: Long, gameday: Int, battle: Int, strategy: VideoProvideStrategy, overrideEnabled: Boolean = false
    ) {
    }

    fun buildStoreStatus(gameday: Int): String {
        config.replayDataStore ?: return "ReplayDataStore not enabled"
        val gamedayData = persistentData.replayDataStore.data[gameday].orEmpty()
        val gameplan = battleorder[gameday] ?: return "Spieltag $gameday: Keine Spiele"
        return "## Aktueller Stand von Spieltag $gameday [$leaguename]:\n" + gameplan.indices.joinToString("\n") {
            "${gameplan[it].joinToString(" vs. ") { u -> "<@${table[u]}>" }}: ${if (gamedayData[it] != null) "✅" else "❌"}"
        }
    }

    inner class PointsManager {
        private val points = mutableMapOf<Int, Int>()

        operator fun get(idx: Int) = points.getOrPut(idx) {
            val isPoints = tierlist.mode.isPoints()
            tierlist.points - picks[idx].orEmpty().filterNot { it.quit || it.noCost }.sumOf {
                if (it.free) tierlist.freepicks[it.tier]!! else if (isPoints) tierlist.prices.getValue(
                    it.tier
                ) else if (tierlist.variableMegaPrice && it.name.isMega) it.tier.substringAfter("#").toInt() else 0
            } - (draftData.teraPick.alreadyPaid[idx] ?: 0)
        }

        operator fun set(idx: Int, points: Int) {
            this.points[idx] = points
        }

        fun add(idx: Int, points: Int) {
            this.points[idx] = this[idx] + points
        }
    }

    fun formatTimeFormatBasedOnDistance(cooldown: Long) = buildString {
        val delay = cooldown - System.currentTimeMillis()
        if (delay >= 24 * 3600 * 1000) append(dayTimeFormat.format(cooldown)).append(" ")
        append(
            (if (config.timer?.stallSeconds == 0 && delay > 15 * 60 * 1000) leagueTimeFormat else leagueTimeFormatSecs).format(
                cooldown
            )
        )
    }

    companion object : CoroutineScope {
        override val coroutineContext = createCoroutineContext("League", Dispatchers.IO)
        val logger = KotlinLogging.logger {}
        val allTimers = mutableMapOf<String, Job>()
        val allStallSecondTimers = mutableMapOf<String, Job>()
        val dayTimeFormat = SimpleDateFormat("dd.MM.")
        val leagueTimeFormat = SimpleDateFormat("HH:mm")
        val leagueTimeFormatSecs = SimpleDateFormat("HH:mm:ss")
        val allMutexes = ConcurrentHashMap<String, Mutex>()
        val queueInteractionData = TestInteractionData(tc = 1099651412742389820)
        val timerScope = createCoroutineScope("LeagueTimer")

        fun getLock(leaguename: String): Mutex = allMutexes.getOrPut(leaguename) { Mutex() }

        suspend inline fun executeOnFreshLock(
            leagueSupplier: () -> League?, onNotFound: () -> Unit = {}, block: League.() -> Unit
        ) {
            executeOnFreshLock(leagueSupplier, { it }, onNotFound, block)
        }

        suspend inline fun <T> executeOnFreshLock(
            supplier: () -> T?, leagueMapper: (T) -> League, onNotFound: () -> Unit = {}, block: T.() -> Unit
        ) {
            val locked = allMutexes.entries.mapNotNullTo(mutableSetOf()) { if (it.value.isLocked) it.key else null }
            val league = supplier() ?: return onNotFound()
            val leaguename = leagueMapper(league).leaguename
            val lock = getLock(leaguename)
            lock.withLock {
                (if (leaguename in locked) supplier()!! else league).apply {
                    logger.info("Time in lock: " + measureTime {
                        block()
                        leagueMapper(this).lockCleanup()
                    })
                }

            }
        }

        suspend inline fun executeOnFreshLock(name: String, block: League.() -> Unit) = getLock(name).withLock {
            val league = db.getLeague(name) ?: return@withLock logger.error("ExecuteOnFreshLock failed for $name")
            league.block()
            league.lockCleanup()
        }

        suspend fun League.lockCleanup() {
            if (shouldSave) {
                db.league.updateOne(this)
            }
        }

        suspend fun executeTimerOnRefreshedVersion(name: String) {
            executeOnFreshLock(name) {
                if (draftData.timer.cooldown <= System.currentTimeMillis()) afterPickOfficial(
                    data = NextPlayerData.Moved(
                        SkipReason.REALTIMER, current
                    )
                )
            }
        }

        context(InteractionData)
        suspend inline fun executePickLike(block: League.() -> Unit) {
            executeOnFreshLock({ byCommand() }, { it.first }, {
                if (!replied) {
                    reply(
                        "Es läuft zurzeit kein Draft in diesem Channel!", ephemeral = true
                    )
                }
            }) {
                // this is only needed when timerSkipMode is AFTER_DRAFT_UNORDERED
                if (first.pseudoEnd && first.afterTimerSkipMode == AFTER_DRAFT_UNORDERED) {
                    // BypassCurrentPlayerData can only be Yes here
                    first.currentOverride = (second as BypassCurrentPlayerData.Yes).user
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
                if (pseudoEnd && afterTimerSkipMode == AFTER_DRAFT_UNORDERED) {
                    val data = afterTimerSkipMode.run { bypassCurrentPlayerCheck(uid) }
                    when (data) {
                        is BypassCurrentPlayerData.Yes -> {
                            return@run this to data
                        }

                        BypassCurrentPlayerData.No -> {
                            reply("Du hast keine offenen Picks mehr!")
                            return null
                        }
                    }
                }
                if (!isCurrentCheck(uid)) {
                    reply("Du bist nicht dran!", ephemeral = true)
                    return null
                }
                this to BypassCurrentPlayerData.No
            }
        }

        suspend fun onlyChannel(tc: Long) =
            db.league.find(League::draftState ne DraftState.OFF, League::tcid eq tc).first()

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
data class AllowedData(@Contextual val u: Long, var mention: Boolean = false, var teammate: Boolean = false)

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
        val englishName = NameConventionsDB.getSDTranslation(
            pokemonofficial, league.guild, english = true
        )!!.tlName
        pokemon.condAppend(pokemon != englishName, "/$englishName")
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
    val updrafted: Boolean,
    val tera: Int?
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

data class TierData(val specified: String, val official: String, val points: Int?)

sealed interface TimerSkipMode {

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

interface DuringTimerSkipMode : TimerSkipMode

interface AfterTimerSkipMode : TimerSkipMode

@Serializable
data object NEXT_PICK : DuringTimerSkipMode {
    override suspend fun League.afterPick(data: NextPlayerData): TimerSkipResult {
        return if (data.isNormalPick() && hasMovedTurns()) {
            movedTurns().removeFirstOrNull()
            if (pseudoEnd && !hasMovedTurns()) TimerSkipResult.NEXT else TimerSkipResult.SAME
        } else TimerSkipResult.NEXT
    }

    override suspend fun League.getPickRound(): Int = movedTurns().firstOrNull() ?: round
}

@Serializable
data object AFTER_DRAFT_ORDERED : AfterTimerSkipMode {
    override suspend fun League.afterPick(data: NextPlayerData): TimerSkipResult {
        if (pseudoEnd && data.isNormalPick()) {
            movedTurns().removeFirstOrNull()
        }
        populateAfterDraft()
        return TimerSkipResult.NEXT
    }


    override suspend fun League.getPickRound() = if (pseudoEnd) movedTurns().firstOrNull() ?: round
    else round

    private fun League.populateAfterDraft() {
        val order = order[totalRounds]!!
        val coll: MutableCollection<Int> = if (duringTimerSkipMode == NEXT_PICK) mutableSetOf() else mutableListOf()
        for (i in 1..totalRounds) {
            moved.entries.filter { i in it.value }.sortedBy { it.key.indexedBy(originalorder[i]!!) }.forEach {
                coll += it.key
            }
        }
        order += coll
        if (coll.isNotEmpty() && draftState != DraftState.PSEUDOEND) {
            draftState = DraftState.PSEUDOEND
            tc.sendMessage("Die regulären Picks sind nun vorbei, nun werden die fehlenden Picks (in der Reihenfolge wie sie übersprungen wurden) nachgeholt.")
                .queue()
        }
    }
}

@Serializable
data object AFTER_DRAFT_UNORDERED : AfterTimerSkipMode {
    override suspend fun League.afterPick(data: NextPlayerData): TimerSkipResult =
        if (moved.values.any { it.isNotEmpty() }) {
            if (!pseudoEnd) {
                tc.sendMessage("Der Draft wäre jetzt vorbei, aber es gibt noch Spieler, die keinen vollständigen Kader haben! Diese können nun in beliebiger Reihenfolge ihre Picks nachholen. Dies sind:\n" + moved.entries.filter { it.value.isNotEmpty() }
                    .joinToString("\n") { (user, turns) ->
                        "<@${table[user]}>: ${turns.size}${
                            announceData(withTimerAnnounce = false, idx = user)
                        }"
                    }).queue()
                cancelCurrentTimer()
                draftState = DraftState.PSEUDOEND
            }
            TimerSkipResult.NOCONCRETE
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

@OptIn(ExperimentalContracts::class)
fun NextPlayerData.isNormalPick(): Boolean {
    contract {
        returns(true) implies (this@isNormalPick is NextPlayerData.Normal)
    }
    return this is NextPlayerData.Normal
}

@OptIn(ExperimentalContracts::class)
fun NextPlayerData.isMoved(): Boolean {
    contract {
        returns(true) implies (this@isMoved is NextPlayerData.Moved)
    }
    return this is NextPlayerData.Moved
}

enum class SkipReason {
    REALTIMER, SKIP
}

enum class TimerSkipResult {
    NEXT, SAME, NOCONCRETE
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
                vids.singleOrNull() ?: run {
                    val ytLeagues = db.config.only().ytLeagues.keys
                    vids.firstOrNull {
                        val title = it.snippet.title
                        ytLeagues.any { league -> title.contains(league, ignoreCase = true) }
                    }
                }
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
