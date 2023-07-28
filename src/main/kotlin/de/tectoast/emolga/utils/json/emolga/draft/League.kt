package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.bot.EmolgaMain.emolgajda
import de.tectoast.emolga.commands.*
import de.tectoast.emolga.commands.draft.AddToTierlistData
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.DraftTimer
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.automation.structure.DocEntry
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

    private val names: MutableMap<Long, String> = mutableMapOf()


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
    open val additionalSet: AdditionalSet? = null

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

    open fun handlePoints(e: GuildCommandEvent, tlNameNew: String, free: Boolean, tlNameOld: String? = null): Boolean {
        if (!tierlist.mode.withPoints) return false
        if (!free && !tierlist.mode.isPoints()) return false
        val needed = tierlist.getPointsNeeded(tlNameNew)
        val pointsBack = tlNameOld?.let { tierlist.getPointsNeeded(it) } ?: 0
        if (points[current] - needed + pointsBack < 0) {
            e.reply("Dafür hast du nicht genug Punkte!")
            return true
        }
        if (pointsBack == 0 && when (tierlist.mode) {
                TierlistMode.POINTS -> (totalRounds - (picks[current]!!.size + 1)) * tierlist.prices.values.min() > points[current] - needed
                TierlistMode.TIERS_WITH_FREE -> (tierlist.freePicksAmount - (picks[current]!!.count { it.free } + 1)) * tierlist.freepicks.entries.filter { it.key != "#AMOUNT#" }
                    .minOf { it.value } > points[current] - needed

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
        e: GuildCommandEvent,
        specifiedTier: String,
        officialTier: String,
        fromSwitch: Boolean = false
    ): Boolean {
        if (!tierlist.mode.withTiers) return false
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

    suspend fun afterPickOfficial() = timerSkipMode?.afterPick(this) ?: afterPick()


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
        } else {
            val delay = if (cooldown != -1L) cooldown - System.currentTimeMillis() else timer?.calc(timerStart)
            restartTimer(delay)
        }
        isRunning = true
        save()
        logger.info("Started!")
    }

    suspend fun save() = db.drafts.updateOne(this)


    open fun reset() {}

    private fun restartTimer(delay: Long? = timer?.calc(timerStart)) {
        delay ?: return
        cooldown = System.currentTimeMillis() + delay
        logger.info("important".marker, "cooldown = {}", cooldown)
        allTimers[leaguename]?.cancel("Restarting timer")
        allTimers[leaguename] = timerScope.launch {
            delay(delay)
            db.drafts.find(League::leaguename eq this@League.leaguename).first()?.triggerTimer()
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
    }.joinToString(prefix = " (", postfix = ")").let { if (it.length == 3) "" else it }
    /*.condAppend(timerSkipMode?.multiplePicksPossible == true && hasMovedTurns()) {
        movedTurns().size.plus(1).let { " **($it Pick${if (it == 1) "" else "s"})**" }
    }*/

    open fun beforePick(): String? = null
    open fun beforeSwitch(): String? = null
    open fun checkUpdraft(specifiedTier: String, officialTier: String): String? = null

    fun getPossibleTiers(mem: Long = current) = tierlist.prices.toMutableMap().let { possible ->
        picks[mem]!!.forEach { pick ->
            pick.takeUnless { it.name == "???" || it.free }?.let { possible[it.tier] = possible[it.tier]!! - 1 }
        }
        possible
    }.also { manipulatePossibleTiers(it) }

    open fun manipulatePossibleTiers(possible: MutableMap<String, Int>) {}

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
            TierData((tierlist.order.firstOrNull { insertedTier.equals(it, ignoreCase = true) } ?: ""), real)
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
        if (order[round]!!.isEmpty()) {

            if (round == totalRounds) {
                tc.sendMessage("Der Draft ist vorbei!").queue()
                //ndsdoc(tierlist, pokemon, d, mem, tier, round);
                //aslCoachDoc(tierlist, pokemon, d, mem, needed, round, null);
                isRunning = false
                save()
                return true
            }
            round++
            if (order[round]!!.isEmpty()) {
                tc.sendMessage("Da alle bereits ihre Drafts beendet haben, ist der Draft vorbei!").queue()
                save()
                return true
            }
            sendRound()
        }
        return false
    }

    protected open fun getCurrentMention(): String {
        return (allowed[current]?.filter { it.mention }?.joinToString { "<@${it.u}>" }
            ?.let { it + " (für ${getCurrentName()})" } ?: "<@$current>")
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
            else append(
                "Der Pick von ${getCurrentName(oldcurrent)} wurde ".condAppend(skippedBy != null) { "von <@$skippedBy> " } + "${if (isSwitchDraft) "geskippt" else "verschoben"} und deshalb ist jetzt ${
                    getCurrentMention()
                } dran!"
            )
            append(announceData())
        }).queue()
        restartTimer()
        save()
    }

    suspend fun nextPlayer() {
        if (endOfTurn()) return
        current = order[round]!!.nextCurrent()
        cooldownJob?.cancel("Next player")
        announcePlayer()
        restartTimer()
        save()
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
    suspend fun replyPick(e: GuildCommandEvent, pokemon: String, free: Boolean, updrafted: String?) =
        replyGeneral(
            e,
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

    suspend fun getPickRoundOfficial() = timerSkipMode?.getPickRound(this) ?: getPickRound()

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

    companion object {
        val logger: Logger by SLF4J
        val allTimers = mutableMapOf<String, Job>()

        val timerScope = CoroutineScope(Dispatchers.Default + SupervisorJob() + CoroutineExceptionHandler { _, t ->
            logger.error(
                "ERROR EXECUTING TIMER", t
            )
        })

        suspend fun byCommand(e: GuildCommandEvent) = onlyChannel(e.textChannel.idLong)?.apply {
            if (!isCurrentCheck(e.member.idLong)) {
                e.reply("Du bist nicht dran!")
                return null
            }
        }

        suspend fun onlyChannel(tc: Long) = db.drafts.find(League::isRunning eq true, League::tcid eq tc).first()
    }

    enum class TimerReason {
        REALTIMER, SKIP
    }
}

@Serializable
data class AllowedData(val u: Long, var mention: Boolean = false)

sealed class DraftData(
    val league: League,
    val pokemon: String,
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

class PickData(
    league: League,
    pokemon: String,
    tier: String,
    mem: Long,
    indexInRound: Int,
    changedIndex: Int,
    picks: MutableList<DraftPokemon>,
    round: Int,
    memIndex: Int,
    val freePick: Boolean
) : DraftData(league, pokemon, tier, mem, indexInRound, changedIndex, picks, round, memIndex) {
    override val changedOnTeamsiteIndex: Int by lazy {
        with(league) { getTierInsertIndex() }
    }
}


class SwitchData(
    league: League,
    pokemon: String,
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
) : DraftData(league, pokemon, tier, mem, indexInRound, changedIndex, picks, round, memIndex)

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

    operator fun get(member: Long) =
        points.getOrPut(member) {
            with(league) {
                val isPoints = tierlist.mode.isPoints()
                tierlist.points - picks[member]!!.filterNot { it.quit }.sumOf {
                    if (it.free) tierlist.freepicks[it.tier]!! else if (isPoints) tierlist.prices.getValue(
                        it.tier
                    ) else 0
                }
            }
        }

    fun add(member: Long, points: Int) {
        this.points[member] = this[member] + points
    }
}

data class AdditionalSet(val col: String, val existent: String, val yeeted: String)
