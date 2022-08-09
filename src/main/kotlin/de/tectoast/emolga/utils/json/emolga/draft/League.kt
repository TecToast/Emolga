package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.draft.PickData
import de.tectoast.emolga.commands.draft.SwitchData
import de.tectoast.emolga.commands.indexedBy
import de.tectoast.emolga.commands.saveEmolgaJSON
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.DraftTimer
import de.tectoast.emolga.utils.automation.structure.DocEntry
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.draft.Tierlist
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.dv8tion.jda.api.entities.TextChannel
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit


@Serializable
sealed class League {
    val sid: String = "yay"
    val picks: MutableMap<Long, MutableList<DraftPokemon>> = mutableMapOf()
    val battleorder: MutableMap<Int, String> = mutableMapOf()
    val results: MutableMap<String, Long> = mutableMapOf()
    val allowed: MutableMap<Long, Long> = mutableMapOf()
    private val mentions: Map<Long, Long> = emptyMap()
    val guild: Long = -1

    private val finished: MutableList<Int> = mutableListOf()
    var round = 1
    var current = -1L
    private var cooldown = -1L
    val points: MutableMap<Long, Int> = mutableMapOf()

    private val originalorder: Map<Int, MutableList<Int>> = mapOf()

    val order: MutableMap<Int, MutableList<Int>> = mutableMapOf()

    @Transient
    lateinit var tc: TextChannel

    val tierlist: Tierlist by Tierlist.Delegate()
    val isPointBased
        get() = tierlist.isPointBased

    @Transient
    val scheduler: ScheduledExecutorService = ScheduledThreadPoolExecutor(2)

    @Transient
    var cooldownFuture: ScheduledFuture<*>? = null
    private var ended = false
    val members: List<Long>
        get() = table
    private val timer: DraftTimer = DraftTimer.ASL
    val isLastRound: Boolean get() = round == tierlist.rounds

    @Transient
    var isSwitchDraft = false

    val table: MutableList<Long> = mutableListOf()

    open fun isFinishedForbidden() = true

    open fun checkFinishedForbidden(mem: Long): String? = null

    open fun savePick(picks: MutableList<DraftPokemon>, pokemon: String, tier: String) {
        picks.add(DraftPokemon(pokemon, tier))
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


    fun isNotCurrent(user: Long): Boolean {
        if (current == user || user == Constants.FLOID) return false
        return allowed[user]?.equals(current) == false
    }

    fun isPicked(mon: String) = picks.values.any { l -> l.any { it.name == mon } }

    fun handlePoints(e: GuildCommandEvent, needed: Int): Boolean {
        if (isPointBased) {
            if (points[current]!! - needed < 0) {
                e.reply("Dafür hast du nicht genug Punkte!")
                return true
            }/*if (d.isPointBased && (d.getTierlist().rounds - d.round) * d.getTierlist().prices.get(d.getTierlist().order.get(d.getTierlist().order.size() - 1)) > (d.points.get(mem) - needed)) {
        tco.sendMessage(memberr.getAsMention() + " Wenn du dir dieses Pokemon holen würdest, kann dein Kader nicht mehr vervollständigt werden!").queue();
        return;
    }*/
            points[current] = points[current]!! - needed
        }
        return false
    }


    fun nextPlayer() {
        if (order[round]!!.isEmpty()) {
            if (round == tierlist.rounds) {
                tc.sendMessage("Der Draft ist vorbei!").queue()
                ended = true
                //ndsdoc(tierlist, pokemon, d, mem, tier, round);
                //aslCoachDoc(tierlist, pokemon, d, mem, needed, round, null);
                saveEmolgaJSON()
                drafts.remove(this)
                return
            }
            round++
            if (order[round]!!.isEmpty()) {
                tc.sendMessage("Da alle bereits ihre Drafts beendet haben, ist der Draft vorbei!").queue()
                saveEmolgaJSON()
                return
            }
            tc.sendMessage("Runde $round!").queue()
        }
        current = order[round]!!.nextCurrent()
        cooldownFuture!!.cancel(false)
        announcePlayer()
        restartTimer()
        saveEmolgaJSON()
    }

    private fun MutableList<Int>.nextCurrent() = table[this.removeAt(0)]

    fun startDraft(tc: TextChannel, isSwitchDraft: Boolean, fromFile: Boolean) {
        this.tc = tc
        this.isSwitchDraft = isSwitchDraft
        for (member in members) {
            if (fromFile) picks.putIfAbsent(member, mutableListOf())
            else picks[member] = mutableListOf()
            if (isPointBased) points[member] = tierlist.points - picks[member]!!.sumOf { tierlist.prices[it.tier]!! }
        }
        if (!fromFile) {
            order.putAll(originalorder)
            current = order[1]!!.nextCurrent()
            restartTimer()
            sendRound()
            announcePlayer()
        } else {
            finished.forEach { fin -> order.values.forEach { l -> l.removeIf { it == fin } } }
            val delay = if (cooldown != -1L) cooldown - System.currentTimeMillis() else timer.calc()
            restartTimer(delay)
        }
        saveEmolgaJSON()
        drafts.add(this)
    }

    private fun restartTimer(delay: Long = timer.calc()) {
        cooldownFuture = scheduler.schedule(
            { triggerTimer() },
            delay.also { cooldown = System.currentTimeMillis() + it },
            TimeUnit.MILLISECONDS
        )
    }

    private fun sendRound() {
        tc.sendMessage("Runde $round!").queue()
    }

    private fun announcePlayer() {
        if (isPointBased) tc.sendMessage(getMention(current) + " ist dran! (" + points[current] + " mögliche Punkte)")
            .queue() else tc.sendMessage(
            getMention(current) + " ist dran! (Mögliche Tiers: " + getPossibleTiersAsString(current) + ")"
        ).queue()
    }

    fun getPossibleTiers(mem: Long): Map<String, Int> = tierlist.prices.toMutableMap().let { possible ->
        picks[mem]!!.forEach { pick ->
            pick.takeUnless { it.name == "???" }?.let { possible[it.tier] = possible[it.tier]!! - 1 }
        }
        possible
    }

    private fun getPossibleTiersAsString(mem: Long) =
        getPossibleTiers(mem).entries.sortedBy { it.key.indexedBy(tierlist.order) }.filterNot { it.value == 0 }
            .joinToString { "${it.value}x **${it.key}**" }


    private fun getMention(mem: Long) = "<@${mentions[mem] ?: mem}>"
    fun isPickedBy(mon: String, mem: Long): Boolean = picks[mem]!!.any { it.name == mon }
    fun indexInRound(): Int = originalorder[round]!!.indexOf(current.indexedBy(table))
    fun triggerTimer(tr: TimerReason = TimerReason.REALTIMER) {
        if (ended) return
        if (order[round]!!.isEmpty()) {
            if (round == tierlist.rounds) {
                saveEmolgaJSON()
                tc.sendMessage("Der Draft ist vorbei!").queue()
                drafts.remove(this)
                return
            }
            round++
            tc.sendMessage("Runde $round!").queue()
        }/*String msg = tr == TimerReason.REALTIMER ? "**" + current.getEffectiveName() + "** war zu langsam und deshalb ist jetzt " + getMention(order.get(round).get(0)) + " (<@&" + asl.getLongList("roleids").get(getIndex(order.get(round).get(0).getIdLong())) + ">) dran! "
                : "Der Pick von " + current.getEffectiveName() + " wurde " + (isSwitchDraft ? "geskippt" : "verschoben") + " und deshalb ist jetzt " + getMention(order.get(round).get(0)) + " dran!";*/
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
                "(Mögliche Tiers: " + getPossibleTiersAsString(current) + ")"
            )
        }).queue()
        restartTimer()
    }

    fun addFinished(mem: Long) {
        val index = mem.indexedBy(table)
        order.values.forEach { it.remove(index) }
        finished.add(index)
    }

    abstract val docEntry: DocEntry?

    companion object {
        val drafts: MutableList<League> = mutableListOf()

        fun byChannel(e: GuildCommandEvent) = onlyChannel(e.textChannel)?.apply {
            if (isNotCurrent(e.member.idLong)) {
                e.reply("Du bist nicht dran!")
                return null
            }
        }

        fun onlyChannel(tc: TextChannel) = drafts.firstOrNull { it.tc.idLong == tc.idLong }
    }

    enum class TimerReason {
        REALTIMER, SKIP
    }
}