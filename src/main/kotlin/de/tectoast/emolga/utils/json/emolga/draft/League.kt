package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.bot.EmolgaMain.emolgajda
import de.tectoast.emolga.commands.*
import de.tectoast.emolga.commands.draft.AddToTierlistData
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.DraftTimer
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.automation.structure.DocEntry
import de.tectoast.emolga.utils.draft.DraftPlayer
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.draft.TierlistMode
import de.tectoast.emolga.utils.json.db
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.util.SLF4J
import kotlinx.coroutines.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import org.litote.kmongo.Id
import org.litote.kmongo.coroutine.updateOne
import org.litote.kmongo.eq
import org.litote.kmongo.set
import org.litote.kmongo.setTo
import org.slf4j.Logger


@Suppress("MemberVisibilityCanBePrivate")
@Serializable
sealed class League {

    @SerialName("_id")
    @Contextual
    val id: Id<League>? = null
    val sid: String = "yay"
    val leaguename: String = "ERROR"
    var isRunning: Boolean = false
    val picks: MutableMap<Long, MutableList<DraftPokemon>> = mutableMapOf()
    val battleorder: MutableMap<Int, List<List<Int>>> = mutableMapOf()
    val results: MutableMap<String, Long> = mutableMapOf()
    val allowed: MutableMap<Long, MutableSet<AllowedData>> = mutableMapOf()
    val guild = -1L
    var round = 1
    var current = -1L
    private var cooldown = -1L
    var pseudoEnd = false

    abstract val teamsize: Int
    open val gamedays: Int by lazy { battleorder.let { if (it.isEmpty()) table.size - 1 else it.size } }

    @Transient
    val points: PointsManager = PointsManager(this)
    val noAutoStart = false

    val timerStart: Long? = null

    @Transient
    open val timerSkipMode: TimerSkipMode? = null
    val originalorder: Map<Int, List<Int>> = mapOf()

    val order: MutableMap<Int, MutableList<Int>> = mutableMapOf()

    private val moved: MutableMap<Long, MutableList<Int>> = mutableMapOf()

    internal val names: MutableMap<Long, String> = mutableMapOf()


    val tc: TextChannel get() = emolgajda.getTextChannelById(tcid) ?: error("No text channel found for guild $guild")

    var tcid: Long = -1


    @Transient
    private var tlCompanion = Tierlist // workaround

    val tierlist: Tierlist by tlCompanion

    @Transient
    open val pickBuffer = 0


    val cooldownJob: Job? get() = allTimers[leaguename]

    @Transient
    open val allowPickDuringSwitch = false

    @Transient
    open val timer: DraftTimer? = null
    val isLastRound: Boolean get() = round == totalRounds
    val totalRounds by lazy { originalorder.size }

    var isSwitchDraft = false

    val table: List<Long> = listOf()

    val tipgame: TipGame? = null

    val tierorderingComparator by lazy { compareBy<DraftPokemon>({ tierlist.order.indexOf(it.tier) }, { it.name }) }


    val newSystemGap get() = teamsize + pickBuffer + 3

    @Transient
    open val additionalSet: AdditionalSet? = AdditionalSet((gamedays + 4).xc(), "X", "Y")

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
    open fun AddToTierlistData.addMonToTierlist() {}

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


    fun isCurrentCheck(user: Long): Boolean {
        if (current == user || user in Constants.DRAFTADMINS) return true
        return isCurrent(user)
    }

    open fun isCurrent(user: Long): Boolean {
        return allowed[current]?.any { it.u == user } ?: false
    }

    open suspend fun isPicked(mon: String, tier: String? = null) =
        picks.values.any { l -> l.any { !it.quit && it.name.equals(mon, ignoreCase = true) } }

    open fun handlePoints(
        e: GuildCommandEvent,
        tlNameNew: String,
        officialNew: String,
        free: Boolean,
        tier: String,
        tierOld: String? = null
    ): Boolean {
        if (!tierlist.mode.withPoints) return false
        val mega = officialNew.isMega
        if (tierlist.mode.isTiersWithFree() && !(tierlist.variableMegaPrice && mega) && !free) return false
        val cpicks = picks[current]!!
        val currentPicksHasMega = cpicks.any { it.name.isMega }
        if (tierlist.variableMegaPrice && currentPicksHasMega && mega) {
            e.reply("Du kannst nur ein Mega draften!")
            return true
        }
        val needed = tierlist.getPointsNeeded(tier)
        val pointsBack = tierOld?.let { tierlist.getPointsNeeded(it) } ?: 0
        if (points[current] - needed + pointsBack < 0) {
            e.reply("Dafür hast du nicht genug Punkte!")
            return true
        }
        val variableMegaPrice =
            (if (tierlist.variableMegaPrice) (if (!currentPicksHasMega) tierlist.order.mapNotNull {
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
            e.reply("Wenn du dir dieses Pokemon holen würdest, kann dein Kader nicht mehr vervollständigt werden!")
            return true
        }
        points.add(current, pointsBack - needed)
        return false
    }

    open fun handleTiers(
        e: GuildCommandEvent, specifiedTier: String, officialTier: String, fromSwitch: Boolean = false
    ): Boolean {
        if (!tierlist.mode.withTiers || (tierlist.variableMegaPrice && "#" in officialTier)) return false
        val map = getPossibleTiers()
        if (!map.containsKey(specifiedTier)) {
            e.reply("Das Tier `$specifiedTier` existiert nicht!")
            return true
        }
        if (tierlist.order.indexOf(officialTier) < tierlist.order.indexOf(specifiedTier) && (!fromSwitch || map[specifiedTier]!! <= 0)) {
            e.reply("Du kannst ein $officialTier-Mon nicht ins $specifiedTier hochdraften!")
            return true
        }
        if (map[specifiedTier]!! <= 0) {
            if (tierlist.prices[specifiedTier] == 0) {
                e.reply("Ein Pokemon aus dem $specifiedTier-Tier musst du in ein anderes Tier hochdraften!")
                return true
            }
            if (fromSwitch) return false
            e.reply("Du kannst dir kein $specifiedTier-Pokemon mehr picken!")
            return true
        }
        return false
    }

    open suspend fun afterPick() {
        nextPlayer()
    }

    suspend fun afterPickOfficial() = timerSkipMode?.afterPick(this)?.also { save("AfterPickOfficial") } ?: afterPick()


    val draftWouldEnd get() = isLastRound && order[round]!!.isEmpty()

    private fun MutableList<Int>.nextCurrent() = table[this.removeAt(0)]

    suspend fun startDraft(tc: GuildMessageChannel?, fromFile: Boolean, switchDraft: Boolean?) {
        switchDraft?.let { this.isSwitchDraft = it }
        logger.info("Starting draft $leaguename...")
        logger.info(tcid.toString())
        if (names.isEmpty()) names.putAll(emolgajda.getGuildById(this.guild)!!.retrieveMembersByIds(table).await()
            .associate { it.idLong to it.effectiveName })
        logger.info(names.toString())
        tc?.let { this.tcid = it.idLong }
        for (member in table) {
            if (fromFile || isSwitchDraft) picks.putIfAbsent(member, mutableListOf())
            else picks[member] = mutableListOf()
        }
        if (!fromFile) {
            order.clear()
            order.putAll(originalorder.mapValues { it.value.toMutableList() })
            current = order[1]!!.nextCurrent()
            round = 1
            moved.clear()
            pseudoEnd = false
            reset()
            restartTimer()
            sendRound()
            announcePlayer()
            save("StartDraft")
        } else {
            val delay = if (cooldown != -1L) cooldown - System.currentTimeMillis() else timer?.calc(timerStart)
            restartTimer(delay)
        }
        db.drafts.updateOneById(id!!, set(League::isRunning setTo true))
        logger.info("Started!")
    }

    suspend fun save(from: String = "") = withContext(NonCancellable) {
        val l = this@League
        db.drafts.updateOne(l)
            .also { logger.info("Saving... {} isRunning {} FROM {}", l.leaguename, l.isRunning, from) }
    }


    open fun reset() {}

    private fun restartTimer(delay: Long? = timer?.calc(timerStart)) {
        delay ?: return
        cooldown = System.currentTimeMillis() + delay
        logger.info("important".marker, "cooldown = {}", cooldown)
        allTimers[leaguename]?.cancel("Restarting timer")
        allTimers[leaguename] = timerScope.launch {
            delay(delay)
            executeTimerOnRefreshedVersion(this@League.leaguename)
        }
    }

    private fun sendRound() {
        tc.sendMessage("**=== Runde $round ===**").queue()
    }

    open fun announcePlayer() {
        tc.sendMessage("${getCurrentMention()} ist dran!${announceData()}").queue()
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
    }.joinToString(prefix = " (", postfix = ")").let { if (it.length == 3) "" else it }/*.condAppend(timerSkipMode?.multiplePicksPossible == true && hasMovedTurns()) {
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
                if (!forAutocomplete)
                    possible["Mega"] = if (cpicks.none { it.name.isMega }) 1 else 0
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

    fun getTierOf(pokemon: String, insertedTier: String?): TierData? {
        val real = tierlist.getTierOf(pokemon) ?: return null
        return if (insertedTier != null && tierlist.mode.withTiers) {
            TierData(tierlist.order.firstOrNull {
                insertedTier.equals(
                    it,
                    ignoreCase = true
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
        moved.getOrPut(current) { mutableListOf() }.let { if (round !in it) it += round }
    }

    fun hasMovedTurns() = movedTurns().isNotEmpty()
    fun movedTurns() = moved[current] ?: mutableListOf()

    private suspend fun endOfTurn(): Boolean {
        logger.info("End of turn")
        if (order[round]!!.isEmpty()) {
            logger.info("No more players")
            if (round == totalRounds) {
                tc.sendMessage("Der Draft ist vorbei!").queue()
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
                return true
            }
            round++
            if (order[round]!!.isEmpty()) {
                tc.sendMessage("Da alle bereits ihre Drafts beendet haben, ist der Draft vorbei!").queue()
                save("FINISH DRAFT END SAVE")
                return true
            }
            sendRound()
        }
        return false
    }

    internal open fun getCurrentMention(): String {
        val data = allowed[current] ?: return "<@$current>"
        val currentData = data.firstOrNull { it.u == current } ?: AllowedData(current, true)
        val (teammates, other) = data.filter { it.mention && it.u != current }.partition { it.teammate }
        return (if (currentData.mention) "<@$current>" else "**${getCurrentName()}**") +
                teammates.joinToString { "<@${it.u}>" }.ifNotEmpty { ", $it" } +
                other.joinToString { "<@${it.u}>" }.ifNotEmpty { ", ||$it||" }
    }

    internal fun getCurrentName(mem: Long = current) = names[mem]!!

    fun indexInRound(round: Int): Int = originalorder[round]!!.indexOf(current.indexedBy(table))
    suspend fun triggerTimer(tr: TimerReason = TimerReason.REALTIMER, skippedBy: Long? = null) {
        if (!isRunning) return
        if (!isSwitchDraft) triggerMove()
        if (draftWouldEnd && timerSkipMode == TimerSkipMode.AFTER_DRAFT) {
            populateAfterDraft()
        }
        if (endOfTurn()) return
        val oldcurrent = current
        current = order[round]!!.nextCurrent()
        tc.sendMessage(buildString {
            if (tr == TimerReason.REALTIMER) append(
                "**${getCurrentName(oldcurrent)}** war zu langsam und deshalb ist jetzt ${
                    getCurrentMention()
                } dran!"
            )
            else append("Der Pick von ${getCurrentName(oldcurrent)} wurde ".condAppend(skippedBy != null) { "von <@$skippedBy> " } + "${if (isSwitchDraft) "geskippt" else "verschoben"} und deshalb ist jetzt ${
                getCurrentMention()
            } dran!")
            append(announceData())
        }).queue()
        restartTimer()
        save("TIMER SAFE")
    }

    suspend fun nextPlayer() {
        cooldownJob?.cancel("Next player")
        if (endOfTurn()) return
        current = order[round]!!.nextCurrent()
        announcePlayer()
        restartTimer()
        save("NEXT PLAYER SAFE")
    }

    fun addFinished(mem: Long) {
        val index = mem.indexedBy(table)
        order.values.forEach { it.remove(index) }
    }

    @Transient
    open val docEntry: DocEntry? = null

    @Transient
    open val dataSheet: String = "Data"

    fun builder() = RequestBuilder(sid)
    suspend fun replyPick(e: GuildCommandEvent, pokemon: String, free: Boolean, updrafted: String?) = replyGeneral(e,
        "$pokemon ".condAppend(updrafted != null) { "im $updrafted " } + "gepickt!".condAppend(free) { " (Free-Pick, neue Punktzahl: ${points[current]})" })

    suspend fun replyGeneral(e: GuildCommandEvent, msg: String, action: ((ReplyCallbackAction) -> Unit)? = null) {
        e.slashCommandEvent!!.reply(
            "${e.member.asMention} hat${
                if (e.author.idLong != current) " für **${getCurrentName()}**" else ""
            } $msg"
        ).also { action?.invoke(it) }.await()
    }

    suspend fun replyRandomPick(e: GuildCommandEvent, pokemon: String, tier: String) = replyGeneral(
        e, "einen Random-Pick im $tier gemacht und **$pokemon** bekommen!"
    )

    suspend fun replySwitch(e: GuildCommandEvent, oldmon: String, newmon: String) {
        replyGeneral(e, "$oldmon gegen $newmon getauscht!")
    }

    suspend fun replySkip(e: GuildCommandEvent) {
        replyGeneral(e, "den Pick übersprungen!")
    }

    open fun getPickRound() = round

    suspend fun getPickRoundOfficial() =
        timerSkipMode?.getPickRound(this)?.also { save("GET PICK ROUND") } ?: getPickRound()

    fun populateAfterDraft() {
        val order = order[totalRounds]!!
        for (i in 1..totalRounds) {
            moved.forEach { (user, turns) -> if (i in turns) order.add(user.indexedBy(table)) }
        }
        if (order.size > 0) {
            pseudoEnd = true
        }
    }

    open fun provideReplayChannel(jda: JDA): TextChannel? = null
    open fun provideResultChannel(jda: JDA): TextChannel? = null

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
        val gameday =
            if (battleorder.isNotEmpty()) battleorder.asIterable().reversed()
                .firstNotNullOfOrNull {
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
                (0..1).asSequence()
                    .sortedBy { battleusers.indexOf(indices[it]) }.map { game[it].alivePokemon }
                    .toList()
            }
        } ?: run {
            battleind to {
                (0..1).map { game[it].alivePokemon }
                    .let { if (u1IsSecond) it.reversed() else it }
            }
        }
        return GamedayData(
            gameday,
            battleindex,
            u1IsSecond,
            numbers
        )
    }

    companion object {
        val logger: Logger by SLF4J
        val allTimers = mutableMapOf<String, Job>()

        val timerScope = CoroutineScope(Dispatchers.Default + SupervisorJob() + CoroutineExceptionHandler { _, t ->
            logger.error(
                "ERROR EXECUTING TIMER", t
            )
        })

        suspend fun executeTimerOnRefreshedVersion(name: String) {
            val league =
                db.drafts.findOne(League::leaguename eq name) ?: return Command.sendToMe("League $name not found")
            league.triggerTimer()
        }

        suspend fun byCommand(e: GuildCommandEvent) = onlyChannel(e.textChannel.idLong)?.apply {
            if (!isCurrentCheck(e.member.idLong)) {
                e.reply("Du bist nicht dran!", ephemeral = true)
                return null
            }
        }

        suspend fun onlyChannel(tc: Long) = db.drafts.find(League::isRunning eq true, League::tcid eq tc).first()
    }

    enum class TimerReason {
        REALTIMER, SKIP
    }

}

val String.isMega get() = "-Mega" in this

@Serializable
data class AllowedData(val u: Long, var mention: Boolean = false, var teammate: Boolean = false)

sealed class DraftData(
    val league: League,
    val pokemon: String,
    val pokemonofficial: String,
    val tier: String,
    val mem: Long,
    val indexInRound: Int,
    val changedIndex: Int,
    val picks: MutableList<DraftPokemon>,
    val round: Int,
    val memIndex: Int,
) {
    val roundIndex get() = round - 1
    abstract val changedOnTeamsiteIndex: Int
}

@Suppress("unused")
class PickData(
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
    val oldtier: String,
    val oldIndex: Int,
    override val changedOnTeamsiteIndex: Int
) : DraftData(league, pokemon, pokemonofficial, tier, mem, indexInRound, changedIndex, picks, round, memIndex)

data class TierData(val specified: String, val official: String)

enum class TimerSkipMode {


    LAST_ROUND {
        override suspend fun afterPick(l: League) {
            with(l) {
                if (isLastRound && hasMovedTurns()) {
                    announcePlayer()
                } else nextPlayer()
            }
        }

        override suspend fun getPickRound(l: League) = with(l) {
            round.let {
                if (it != totalRounds) it
                else {
                    val mt = movedTurns()
                    if (totalRounds - picks[current]!!.size < mt.size) {
                        mt.removeFirst()
                    } else it
                }
            }
        }
    },
    AFTER_DRAFT {
        override suspend fun afterPick(l: League) = with(l) {
            if (draftWouldEnd) populateAfterDraft()
            nextPlayer()
        }

        override suspend fun getPickRound(l: League): Int = with(l) {
            if (pseudoEnd) {
                movedTurns().removeFirst()
            } else round
        }

    },
    NEXT_PICK {
        override suspend fun afterPick(l: League) {
            with(l) {
                if (hasMovedTurns()) {
                    movedTurns().removeFirstOrNull()
                    announcePlayer()
                } else nextPlayer()
            }
        }

        override suspend fun getPickRound(l: League) = with(l) { movedTurns().firstOrNull() ?: round }
    };


    abstract suspend fun afterPick(l: League)
    abstract suspend fun getPickRound(l: League): Int
}

class PointsManager(val league: League) {
    private val points = mutableMapOf<Long, Int>()

    operator fun get(member: Long) = points.getOrPut(member) {
        with(league) {
            val isPoints = tierlist.mode.isPoints()
            tierlist.points - picks[member]!!.filterNot { it.quit }.sumOf {
                if (it.free) tierlist.freepicks[it.tier]!! else if (isPoints) tierlist.prices.getValue(
                    it.tier
                ) else if (tierlist.variableMegaPrice && it.name.isMega) it.tier.substringAfter("#").toInt() else 0
            }
        }
    }

    fun add(member: Long, points: Int) {
        this.points[member] = this[member] + points
    }
}

data class AdditionalSet(val col: String, val existent: String, val yeeted: String)
