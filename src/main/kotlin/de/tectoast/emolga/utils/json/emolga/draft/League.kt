package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.draft.DraftEvent
import de.tectoast.emolga.commands.draft.PickData
import de.tectoast.emolga.commands.draft.SwitchData
import de.tectoast.emolga.commands.indexedBy
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.DraftTimer
import de.tectoast.emolga.utils.automation.structure.DocEntry
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.draft.Tierlist
import kotlinx.serialization.SerialName
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
    val mentions: Map<Long, Long> = emptyMap()
    val guild: Long = -1

    val finished: MutableList<Long> = mutableListOf()
    var round = 1
    var current = -1L
    var cooldown = -1L
    val points: MutableMap<Long, Int> = mutableMapOf()

    @SerialName("order")
    val originalOrder: Map<Int, MutableList<Long>> = mapOf()

    @Transient
    val order: MutableMap<Int, MutableList<Long>> = mutableMapOf()

    @Transient
    lateinit var tc: TextChannel

    val tierlist: Tierlist by Tierlist.Delegate()
    val isPointBased
        get() = tierlist.isPointBased

    @Transient
    val scheduler: ScheduledExecutorService = ScheduledThreadPoolExecutor(2)

    @Transient
    var cooldownFuture: ScheduledFuture<*>? = null
    var ended = false
    val members: List<Long>
        get() = originalOrder.getValue(1)
    private val timer: DraftTimer = DraftTimer.ASL

    @Transient
    var isSwitchDraft = false

    val table: MutableList<Long> = mutableListOf()

    fun isNotCurrent(user: Long): Boolean {
        if (current == user || user == Constants.FLOID) return false
        return allowed[user]?.equals(current) == false
    }

    fun isPicked(mon: String) = picks.values.any { l -> l.any { it.name == mon } }

    fun handlePoints(e: DraftEvent, needed: Int): Boolean {
        if (isPointBased && points[current]!! - needed < 0) {
            e.reply("Dafür hast du nicht genug Punkte!")
            return true
        }
        /*if (d.isPointBased && (d.getTierlist().rounds - d.round) * d.getTierlist().prices.get(d.getTierlist().order.get(d.getTierlist().order.size() - 1)) > (d.points.get(mem) - needed)) {
        tco.sendMessage(memberr.getAsMention() + " Wenn du dir dieses Pokemon holen würdest, kann dein Kader nicht mehr vervollständigt werden!").queue();
        return;
    }*/
        if (isPointBased) points[current] = points[current]!! - needed
        return false
    }

    open fun savePick(picks: MutableList<DraftPokemon>, pokemon: String, tier: String) {
        picks.add(DraftPokemon(pokemon, tier))
    }

    open fun pickDoc(data: PickData) {

    }

    open fun switchDoc(data: SwitchData) {

    }

    fun nextPlayer() {
        if (order[round]!!.size == 0) {
            if (round == tierlist.rounds) {
                tc.sendMessage("Der Draft ist vorbei!").queue()
                ended = true
                //ndsdoc(tierlist, pokemon, d, mem, tier, round);
                //aslCoachDoc(tierlist, pokemon, d, mem, needed, round, null);
                Command.saveEmolgaJSON()
                drafts.remove(this)
                return
            }
            round++
            if (order[round]!!.size == 0) {
                tc.sendMessage("Da alle bereits ihre Drafts beendet haben, ist der Draft vorbei!").queue()
                Command.saveEmolgaJSON()
                return
            }
            tc.sendMessage("Runde $round!").queue()
        }
        current = order[round]!!.removeAt(0)
        cooldownFuture!!.cancel(false)
        announcePlayer()
        restartTimer()
        Command.saveEmolgaJSON()
    }

    fun startDraft(tc: TextChannel, isSwitchDraft: Boolean, fromFile: Boolean) {
        this.tc = tc
        this.isSwitchDraft = isSwitchDraft
        order.putAll(originalOrder)
        for (member in members) {
            if (fromFile)
                picks.putIfAbsent(member, mutableListOf())
            else picks[member] = mutableListOf()
            if (isPointBased)
                points[member] = tierlist.points - picks[member]!!.sumOf { tierlist.prices[it.tier]!! }
        }
        if (!fromFile) {
            current = order[1]!!.removeAt(0)
            restartTimer()
            sendRound()
            announcePlayer()
        } else {
            var x = 0
            for (member in order[round]!!) {
                x++
                if (current == member) break
            }
            if (x > 0) {
                order[round]!!.subList(0, x).clear()
            }
            finished.forEach { fin -> order.values.forEach { l -> l.removeIf { it == fin } } }
            val delay = if (cooldown != -1L) cooldown - System.currentTimeMillis() else timer.calc()
            restartTimer(delay)
        }
        Command.saveEmolgaJSON()
        drafts.add(this)
    }

    fun restartTimer(delay: Long = timer.calc()) {
        cooldownFuture = scheduler.schedule(
            { triggerTimer() },
            delay.also { cooldown = System.currentTimeMillis() + it },
            TimeUnit.MILLISECONDS
        )
    }

    fun sendRound() {
        tc.sendMessage("Runde $round!").queue()
    }

    fun announcePlayer() {
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

    fun getPossibleTiersAsString(mem: Long) =
        getPossibleTiers(mem).entries.sortedBy { it.key.indexedBy(tierlist.order) }.filterNot { it.value == 0 }
            .joinToString { "${it.value}x **${it.key}**" }


    fun getMention(mem: Long) = "<@${mentions[mem] ?: mem}>"
    fun isPickedBy(mon: String, mem: Long): Boolean = picks[mem]!!.any { it.name == mon }
    fun indexInRound(): Int = originalOrder[round]!!.indexOf(current)
    fun triggerTimer(tr: TimerReason = TimerReason.REALTIMER) {
        if (ended) return
        if (order[round]!!.size == 0) {
            if (round == tierlist.rounds) {
                Command.saveEmolgaJSON()
                tc.sendMessage("Der Draft ist vorbei!").queue()
                drafts.remove(this)
                return
            }
            round++
            tc.sendMessage("Runde $round!").queue()
        }
        /*String msg = tr == TimerReason.REALTIMER ? "**" + current.getEffectiveName() + "** war zu langsam und deshalb ist jetzt " + getMention(order.get(round).get(0)) + " (<@&" + asl.getLongList("roleids").get(getIndex(order.get(round).get(0).getIdLong())) + ">) dran! "
                : "Der Pick von " + current.getEffectiveName() + " wurde " + (isSwitchDraft ? "geskippt" : "verschoben") + " und deshalb ist jetzt " + getMention(order.get(round).get(0)) + " dran!";*/
        var msg = if (tr == TimerReason.REALTIMER) "**<@$current>** war zu langsam und deshalb ist jetzt " + getMention(
            order[round]!![0]
        ) + " dran! " else "Der Pick von <@" + current + "> wurde " + (if (isSwitchDraft) "geskippt" else "verschoben") + " und deshalb ist jetzt " + getMention(
            order[round]!![0]
        ) + " dran!"
        current = order[round]!!.removeAt(0)
        msg += if (isPointBased) "(" + points[current] + " mögliche Punkte)" else "(Mögliche Tiers: " + getPossibleTiersAsString(
            current
        ) + ")"
        tc.sendMessage(msg).queue()
        restartTimer()
    }

    abstract val docEntry: DocEntry?

    companion object {
        val drafts: MutableList<League> = mutableListOf()

        fun byChannel(tc: TextChannel, mem: Long, e: DraftEvent) = onlyChannel(tc)?.apply {
            if (isNotCurrent(mem)) {
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