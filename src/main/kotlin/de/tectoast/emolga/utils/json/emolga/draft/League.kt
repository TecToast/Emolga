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
    val battleorder: MutableMap<Int, String> = mutableMapOf()
    val results: MutableMap<String, Long> = mutableMapOf()
    val allowed: MutableMap<Long, MutableList<AllowedData>> = mutableMapOf()
    val guild: Long = -1
    var round = 1
    var current = -1L
    private var cooldown = -1L
    val points: MutableMap<Long, Int> = mutableMapOf()

    private val originalorder: Map<Int, List<Int>> = mapOf()

    val order: MutableMap<Int, MutableList<Int>> = mutableMapOf()

    private val moved: MutableMap<Long, MutableList<Int>> = mutableMapOf()

    private val names: MutableMap<Long, String> = mutableMapOf()


    val tc: TextChannel get() = emolgajda.getTextChannelById(tcid) ?: error("No text channel found for guild $guild")

    private var tcid: Long = -1

    val tierlist: Tierlist by Tierlist
    val isPointBased
        get() = tierlist.isPointBased

    @Transient
    var cooldownJob: Job? = null

    @Transient
    open val allowPickDuringSwitch = false
    val members: List<Long>
        get() = table
    abstract val timer: DraftTimer
    val isLastRound: Boolean get() = round == tierlist.rounds

    var isSwitchDraft = false

    val table: MutableList<Long> = mutableListOf()

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
                TierlistMode.POINTS -> (tierlist.rounds - (picks[current]!!.size + 1)) * tierlist.prices.values.min() > points[current]!! - needed
                TierlistMode.MIX -> (tierlist.freePicksAmount - (picks[current]!!.count { it.free } + 1)) * tierlist.freepicks.entries.filter { it.key != "#AMOUNT#" }
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
        if (isLastRound && hasMovedTurns()) {
            announcePlayer()
        } else nextPlayer()
    }


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
        if (names.isEmpty()) names.putAll(emolgajda.getGuildById(this.guild)!!.retrieveMembersByIds(members).await()
            .associate { it.idLong to it.effectiveName })
        logger.info(names.toString())
        if (tc != null) this.tcid = tc.idLong
        for (member in members) {
            if (fromFile || isSwitchDraft) picks.putIfAbsent(member, mutableListOf())
            else picks[member] = mutableListOf()
            val isPoints = tierlist.mode.isPoints()
            if (!tierlist.mode.isTiers()) points[member] =
                tierlist.points - picks[member]!!.sumOf {
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
            restartTimer()
            sendRound()
            announcePlayer()
        } else {
            val delay = if (cooldown != -1L) cooldown - System.currentTimeMillis() else timer.calc()
            restartTimer(delay)
        }
        isRunning = true
        saveEmolgaJSON()
        logger.info("Started!")
    }

    private fun restartTimer(delay: Long = timer.calc()) {
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
        if (isPointBased) tc.sendMessage(getMention(current) + " ist dran! (" + points[current] + " mögliche Punkte)")
            .queue() else tc.sendMessage(
            getMention(current) + " ist dran! (Mögliche Tiers: " + getPossibleTiersAsString() + ")"
        ).queue()
    }

    fun getPossibleTiers(): Map<String, Int> = tierlist.prices.toMutableMap().let { possible ->
        picks[current]!!.forEach { pick ->
            pick.takeUnless { it.name == "???" || it.free }?.let { possible[it.tier] = possible[it.tier]!! - 1 }
        }
        possible
    }

    private fun getPossibleTiersAsString() =
        getPossibleTiers().entries.sortedBy { it.key.indexedBy(tierlist.order) }.filterNot { it.value == 0 }
            .joinToString { "${it.value}x **Tier ${it.key}**" }.let { str ->
                if (tierlist.mode.isMix()) str + "; ${tierlist.freePicksAmount - picks[current]!!.count { it.free }}x **Free Pick**"
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

    fun getTierInsertIndex(picks: List<DraftPokemon>, tier: String): Int {
        var index = 0
        for (entry in tierlist.prices.entries) {
            if (entry.key == tier) {
                return picks.count { !it.free && it.tier == tier } + index - 1
            }
            index += entry.value
        }
        error("Tier $tier not found by user $current")
    }

    fun triggerMove() {
        moved.getOrPut(current) { mutableListOf() }.add(round)
    }

    fun hasMovedTurns() = movedTurns().isNotEmpty()
    fun movedTurns() = moved[current] ?: mutableListOf()

    private fun endOfTurn(): Boolean {
        if (order[round]!!.isEmpty()) {

            if (round == tierlist.rounds) {
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

    protected open fun getMention(mem: Long): String {
        return allowed[mem]?.firstOrNull { it.mention }?.u?.let { "<@$it> (für ${getName(mem)})" } ?: "<@$mem>"
    }

    private fun getName(mem: Long) = names[mem]!!

    fun isPickedBy(mon: String, mem: Long): Boolean = picks[mem]!!.any { it.name == mon }
    fun indexInRound(round: Int): Int = originalorder[round]!!.indexOf(current.indexedBy(table))
    fun triggerTimer(tr: TimerReason = TimerReason.REALTIMER) {
        logger.info("TriggerTimer 1")
        if (!isRunning) return
        logger.info("TriggerTimer 2")
        if (!isSwitchDraft) triggerMove()
        logger.info("TriggerTimer 3")
        if (endOfTurn()) return/*String msg = tr == TimerReason.REALTIMER ? "**" + current.getEffectiveName() + "** war zu langsam und deshalb ist jetzt " + getMention(order.get(round).get(0)) + " (<@&" + asl.getLongList("roleids").get(getIndex(order.get(round).get(0).getIdLong())) + ">) dran! "
                : "Der Pick von " + current.getEffectiveName() + " wurde " + (isSwitchDraft ? "geskippt" : "verschoben") + " und deshalb ist jetzt " + getMention(order.get(round).get(0)) + " dran!";*/

        logger.info("TriggerTimer 4")
        val oldcurrent = current
        current = order[round]!!.nextCurrent()
        tc.sendMessage(buildString {
            if (tr == TimerReason.REALTIMER) append(
                "**<@$oldcurrent>** war zu langsam und deshalb ist jetzt ${
                    getMention(current)
                } dran!"
            )
            else append(
                "Der Pick von <@$oldcurrent> wurde ${if (isSwitchDraft) "geskippt" else "verschoben"} und deshalb ist jetzt ${
                    getMention(current)
                } dran!"
            )
            if (isPointBased) append(" (${points[current]} mögliche Punkte)")
            else append(
                " (Mögliche Tiers: " + getPossibleTiersAsString() + ")"
            )
        }).queue()
        restartTimer()
        saveEmolgaJSON()
    }

    fun addFinished(mem: Long) {
        val index = mem.indexedBy(table)
        order.values.forEach { it.remove(index) }
    }

    abstract val docEntry: DocEntry?

    fun builder() = RequestBuilder(sid)
    suspend fun replyPick(e: GuildCommandEvent, pokemon: String, mem: Long, free: Boolean) {
        e.slashCommandEvent!!.reply(
            "${e.member.asMention} hat${
                if (e.author.idLong != mem) " für **${getName(mem)}**" else ""
            } $pokemon gepickt!".condAppend(free, " (Free-Pick)")
        ).await()
    }

    suspend fun replyRandomPick(e: GuildCommandEvent, pokemon: String, mem: Long, tier: String) {
        e.slashCommandEvent!!.reply(
            if (e.author.idLong != mem) "${e.member.asMention} hat für **${getName(mem)}** einen Random-Pick gemacht und **$pokemon** bekommen!" else "**<@${e.member.asMention}>** hat aus dem $tier-Tier ein **$pokemon** bekommen!"
        ).await()
    }

    suspend fun replySwitch(e: GuildCommandEvent, oldmon: String, newmon: String) {
        e.slashCommandEvent!!.reply("${e.member.asMention} hat ".condAppend(e.author.idLong != current) {
            "für **${getName(current)}** "
        } + "$oldmon gegen $newmon getauscht!").await()
    }

    suspend fun replySkip(e: GuildCommandEvent) {
        e.slashCommandEvent!!.reply("${e.member.asMention} hat ".condAppend(e.author.idLong != current) {
            "für **${getName(current)}** "
        } + "den Pick übersprungen!").await()
    }

    open fun getPickRound() = round


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
                    val delay = if (cooldown != -1L) cooldown - System.currentTimeMillis() else timer.calc()
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
)

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