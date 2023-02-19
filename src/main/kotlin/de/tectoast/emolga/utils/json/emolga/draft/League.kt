package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.bot.EmolgaMain.emolgajda
import de.tectoast.emolga.commands.*
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.DraftTimer
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.automation.structure.DocEntry
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.draft.TierlistMode
import de.tectoast.emolga.utils.json.Emolga
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.util.SLF4J
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import org.slf4j.Logger


@Serializable
sealed class League {
    val name: String
        get() = Emolga.get.drafts.reverseGet(this)!!
    var isRunning: Boolean = false
    val sid: String = "yay"
    val picks: MutableMap<Long, MutableList<DraftPokemon>> = mutableMapOf()
    val battleorder: MutableMap<Int, List<List<Int>>> = mutableMapOf()
    val results: MutableMap<String, Long> = mutableMapOf()
    val allowed: MutableMap<Long, MutableSet<AllowedData>> = mutableMapOf()
    val guild = -1L
    var round = 1
    var current = -1L
    private var cooldown = -1L

    @Transient
    val points: MutableMap<Long, Int> = mutableMapOf()
    val noAutoStart = false
    val timerStart: Long? = null

    @Transient
    open val timerSkipMode: TimerSkipMode? = null
    private val originalorder: Map<Int, List<Int>> = mapOf()

    val order: MutableMap<Int, MutableList<Int>> = mutableMapOf()

    private val moved: MutableMap<Long, MutableList<Int>> = mutableMapOf()

    private val names: MutableMap<Long, String> = mutableMapOf()


    val tc: TextChannel get() = emolgajda.getTextChannelById(tcid) ?: error("No text channel found for guild $guild")

    private var tcid: Long = -1

    val tierlist: Tierlist by Tierlist
    val isPointBased
        get() = tierlist.isPointBased

    val monCount by lazy { picks.values.first().size }

    @Transient
    open val pickBuffer = 0

    @Transient
    var cooldownJob: Job? = null

    @Transient
    open val allowPickDuringSwitch = false
    abstract val timer: DraftTimer
    val isLastRound: Boolean get() = round == totalRounds
    val totalRounds by lazy { originalorder.size }

    var isSwitchDraft = false

    val table: MutableList<Long> = mutableListOf()

    val tipgame: TipGame? = null

    open fun isFinishedForbidden() = true

    open fun checkFinishedForbidden(mem: Long): String? = null

    open fun savePick(picks: MutableList<DraftPokemon>, pokemon: String, tier: String, free: Boolean) {
        picks.add(DraftPokemon(pokemon, tier, free))
    }

    open fun saveSwitch(picks: MutableList<DraftPokemon>, oldmon: String, newmon: String, tierOf: String) {
        picks.first { it.name == oldmon }.apply {
            this.name = newmon
            this.tier = tierlist.getTierOf(newmon)
        }
    }

    open fun pickDoc(data: PickData) {

    }

    open fun switchDoc(data: SwitchData) {

    }


    fun isCurrentCheck(user: Long): Boolean {
        if (current == user || user in listOf(Constants.FLOID, Constants.DASORID)) return true
        return isCurrent(user)
    }

    open fun isCurrent(user: Long): Boolean {
        return allowed[current]?.any { it.u == user } ?: false
    }

    fun isPicked(mon: String) = picks.values.any { l -> l.any { it.name.equals(mon, ignoreCase = true) } }

    open fun handlePoints(e: GuildCommandEvent, needed: Int, free: Boolean): Boolean {
        if (tierlist.mode.isTiers()) return false
        if (!free && !tierlist.mode.isPoints()) return false
        if (points[current]!! - needed < 0) {
            e.reply("Dafür hast du nicht genug Punkte!")
            return true
        }
        if (when (tierlist.mode) {
                TierlistMode.POINTS -> (totalRounds - (picks[current]!!.size + 1)) * tierlist.prices.values.min() > points[current]!! - needed
                TierlistMode.TIERS_WITH_FREE -> (tierlist.freePicksAmount - (picks[current]!!.count { it.free } + 1)) * tierlist.freepicks.entries.filter { it.key != "#AMOUNT#" }
                    .minOf { it.value } > points[current]!! - needed

                else -> false
            }
        ) {
            e.reply("Wenn du dir dieses Pokemon holen würdest, kann dein Kader nicht mehr vervollständigt werden!")
            return true
        }
        points[current] = points[current]!! - needed
        return false
    }

    open fun handleTiers(e: GuildCommandEvent, tier: String, origtier: String): Boolean {
        if (tierlist.mode.isPoints()) return false
        val map = getPossibleTiers()
        if (!map.containsKey(tier)) {
            e.reply("Das Tier `$tier` existiert nicht!")
            return true
        }
        if (tierlist.order.indexOf(origtier) < tierlist.order.indexOf(tier)) {
            e.reply("Du kannst ein $origtier-Mon nicht ins $tier hochdraften!")
            return true
        }
        if (map[tier]!! <= 0) {
            if (tierlist.prices[tier] == 0) {
                e.reply("Ein Pokemon aus dem $tier-Tier musst du in ein anderes Tier hochdraften!")
                return true
            }
            e.reply("Du kannst dir kein $tier-Pokemon mehr picken!")
            return true
        }
        return false
    }

    open fun afterPick() {
        nextPlayer()
    }

    fun afterPickOfficial() = timerSkipMode?.afterPick(this) ?: afterPick()


    fun nextPlayer() {
        if (endOfTurn()) return
        current = order[round]!!.nextCurrent()
        cooldownJob?.cancel("Timer runs out")
        announcePlayer()
        restartTimer()
        saveEmolgaJSON()
    }

    private fun MutableList<Int>.nextCurrent() = table[this.removeAt(0)]

    suspend fun startDraft(tc: TextChannel?, fromFile: Boolean, switchDraft: Boolean?) {
        switchDraft?.let { this.isSwitchDraft = it }
        logger.info("Starting draft $name...")
        logger.info(tcid.toString())
        if (names.isEmpty()) names.putAll(emolgajda.getGuildById(this.guild)!!.retrieveMembersByIds(table).await()
            .associate { it.idLong to it.effectiveName })
        logger.info(names.toString())
        tc?.let { this.tcid = it.idLong }
        for (member in table) {
            if (fromFile || isSwitchDraft) picks.putIfAbsent(member, mutableListOf())
            else picks[member] = mutableListOf()
            val isPoints = tierlist.mode.isPoints()
            if (tierlist.mode.withPoints) points[member] = tierlist.points - picks[member]!!.sumOf {
                if (it.free) tierlist.freepicks[it.tier]!! else if (isPoints) tierlist.prices.getValue(
                    it.tier
                ) else 0
            }
        }
        if (!fromFile) {
            order.clear()
            order.putAll(originalorder.mapValues { it.value.toMutableList() })
            current = order[1]!!.nextCurrent()
            round = 1
            moved.clear()
            reset()
            restartTimer()
            sendRound()
            announcePlayer()
        } else {
            val delay = if (cooldown != -1L) cooldown - System.currentTimeMillis() else timer.calc(timerStart)
            restartTimer(delay)
        }
        isRunning = true
        saveEmolgaJSON()
        logger.info("Started!")
    }

    open fun reset() {}

    private fun restartTimer(delay: Long = timer.calc(timerStart)) {
        cooldown = System.currentTimeMillis() + delay
        logger.info("important".marker, "cooldown = {}", cooldown)
        cooldownJob = timerScope.launch {
            delay(delay)
            triggerTimer()
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
            if (withTiers) add("Mögliche Tiers: " + getPossibleTiersAsString())
            if (withPoints) add(
                "${points[current]} mögliche Punkte".condAppend(
                    isTiersWithFree(), " für Free-Picks"
                )
            )
        }
    }.joinToString(prefix = " (", postfix = ")")
        .condAppend(hasMovedTurns()) { "**${movedTurns().firstOrNull()} Picks**" }

    open fun beforePick(): String? = null

    fun getPossibleTiers(mem: Long = current) = tierlist.prices.toMutableMap().let { possible ->
        picks[mem]!!.forEach { pick ->
            pick.takeUnless { it.name == "???" || it.free }?.let { possible[it.tier] = possible[it.tier]!! - 1 }
        }
        possible
    }

    fun getPossibleTiersAsString(mem: Long = current) =
        getPossibleTiers(mem).entries.sortedBy { it.key.indexedBy(tierlist.order) }.filterNot { it.value == 0 }
            .joinToString { "${it.value}x **".condAppend(it.key.toIntOrNull() != null, "Tier ") + "${it.key}**" }
            .let { str ->
                if (tierlist.mode.isTiersWithFree()) str + "; ${tierlist.freePicksAmount - picks[mem]!!.count { it.free }}x **Free Pick**"
                else str
            }

    fun getTierOf(pokemon: String, insertedTier: String?): Pair<String, String> {
        val real = tierlist.getTierOf(pokemon)
        return if (insertedTier != null && !isPointBased) {
            (tierlist.order.firstOrNull { insertedTier.equals(it, ignoreCase = true) } ?: "") to real
        } else {
            real to real
        }
    }

    fun getTierInsertIndex(data: PickData): Int {
        var index = 0
        for (entry in tierlist.prices.entries) {
            if (entry.key == data.tier) {
                return data.picks.count { !it.free && it.tier == data.tier } + index - 1
            }
            index += entry.value
        }
        error("Tier ${data.tier} not found by user $current")
    }

    fun triggerMove() {
        moved.getOrPut(current) { mutableListOf() }.add(round)
    }

    fun hasMovedTurns() = movedTurns().isNotEmpty()
    fun movedTurns() = moved[current] ?: mutableListOf()

    private fun endOfTurn(): Boolean {
        if (order[round]!!.isEmpty()) {

            if (round == totalRounds) {
                tc.sendMessage("Der Draft ist vorbei!").queue()
                //ndsdoc(tierlist, pokemon, d, mem, tier, round);
                //aslCoachDoc(tierlist, pokemon, d, mem, needed, round, null);
                isRunning = false
                saveEmolgaJSON()
                return true
            }
            round++
            if (order[round]!!.isEmpty()) {
                tc.sendMessage("Da alle bereits ihre Drafts beendet haben, ist der Draft vorbei!").queue()
                saveEmolgaJSON()
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

    private fun getCurrentName() = names[current]!!

    fun isPickedBy(mon: String, mem: Long): Boolean = picks[mem]!!.any { it.name == mon }
    fun indexInRound(round: Int): Int = originalorder[round]!!.indexOf(current.indexedBy(table))
    fun triggerTimer(tr: TimerReason = TimerReason.REALTIMER) {
        if (!isRunning) return
        if (!isSwitchDraft) triggerMove()
        if (endOfTurn()) return
        val oldcurrent = current
        current = order[round]!!.nextCurrent()
        tc.sendMessage(buildString {
            if (tr == TimerReason.REALTIMER) append(
                "**<@$oldcurrent>** war zu langsam und deshalb ist jetzt ${
                    getCurrentMention()
                } dran!"
            )
            else append(
                "Der Pick von <@$oldcurrent> wurde ${if (isSwitchDraft) "geskippt" else "verschoben"} und deshalb ist jetzt ${
                    getCurrentMention()
                } dran!"
            )
            append(announceData())
        }).queue()
        restartTimer()
        saveEmolgaJSON()
    }

    fun addFinished(mem: Long) {
        val index = mem.indexedBy(table)
        order.values.forEach { it.remove(index) }
    }

    @Transient
    open val docEntry: DocEntry? = null

    val dataSheet: String? = null

    fun builder() = RequestBuilder(sid)
    suspend fun replyPick(e: GuildCommandEvent, pokemon: String, free: Boolean) =
        replyGeneral(e, "$pokemon gepickt!".condAppend(free) { " (Free-Pick, neue Punktzahl: ${points[current]})" })

    suspend fun replyGeneral(e: GuildCommandEvent, msg: String) {
        e.slashCommandEvent!!.reply(
            "${e.member.asMention} hat${
                if (e.author.idLong != current) " für **${getCurrentName()}**" else ""
            } $msg"
        ).await()
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

    fun getPickRoundOfficial() = timerSkipMode?.getPickRound(this) ?: getPickRound()


    companion object {
        val logger: Logger by SLF4J

        var timerScope = scopebuilder()

        private fun scopebuilder() =
            CoroutineScope(Dispatchers.Default + SupervisorJob() + CoroutineExceptionHandler { _, t ->
                logger.error(
                    "ERROR EXECUTING TIMER", t
                )
            })

        fun restart() {
            timerScope.cancel("Restart")
            timerScope = scopebuilder()
            Emolga.get.drafts.values.forEach {
                with(it) {
                    if (!isRunning) return@forEach
                    val delay = if (cooldown != -1L) cooldown - System.currentTimeMillis() else timer.calc(timerStart)
                    restartTimer(delay)
                }
            }
        }

        fun byChannel(e: GuildCommandEvent) = onlyChannel(e.textChannel.idLong)?.apply {
            if (!isCurrentCheck(e.member.idLong)) {
                e.reply("Du bist nicht dran!")
                return null
            }
        }

        fun onlyChannel(tc: Long) = Emolga.get.drafts.values.firstOrNull { it.isRunning && it.tc.idLong == tc }
        fun byGuild(gid: Long) = Emolga.get.drafts.values.firstOrNull { it.guild == gid }
    }

    enum class TimerReason {
        REALTIMER, SKIP
    }
}

@Serializable
data class AllowedData(val u: Long, var mention: Boolean = false)

sealed class DraftData(
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
}

class PickData(
    pokemon: String,
    tier: String,
    mem: Long,
    indexInRound: Int,
    changedIndex: Int,
    picks: MutableList<DraftPokemon>,
    round: Int,
    memIndex: Int,
    val freePick: Boolean
) : DraftData(pokemon, tier, mem, indexInRound, changedIndex, picks, round, memIndex)

@Suppress("unused")
class SwitchData(
    pokemon: String,
    tier: String,
    mem: Long,
    indexInRound: Int,
    changedIndex: Int,
    picks: MutableList<DraftPokemon>,
    round: Int,
    memIndex: Int,
    val oldmon: String,
    val oldtier: String
) : DraftData(pokemon, tier, mem, indexInRound, changedIndex, picks, round, memIndex)

enum class TimerSkipMode {
    AT_THE_END {
        override fun afterPick(l: League) {
            with(l) {
                if (isLastRound && hasMovedTurns()) {
                    announcePlayer()
                } else nextPlayer()
            }
        }

        override fun getPickRound(l: League) = with(l) {
            round.let {
                if (it != totalRounds) it
                else {
                    val mt = movedTurns()
                    if (totalRounds - picks.size < mt.size) {
                        mt.removeFirst()
                    } else it
                }
            }
        }
    },
    NEXT_PICK {
        override fun afterPick(l: League) {
            with(l) {
                if (hasMovedTurns()) {
                    announcePlayer()
                    movedTurns().removeFirstOrNull()
                } else nextPlayer()
            }
        }

        override fun getPickRound(l: League) = with(l) { movedTurns().firstOrNull() ?: round }
    };

    abstract fun afterPick(l: League)
    abstract fun getPickRound(l: League): Int
}
