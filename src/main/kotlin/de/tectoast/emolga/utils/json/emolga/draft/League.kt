package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.commands.*
import de.tectoast.emolga.commands.draft.PickData
import de.tectoast.emolga.commands.draft.SwitchData
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.DraftTimer
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.automation.structure.DocEntry
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.json.Emolga
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.util.SLF4J
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.dv8tion.jda.api.entities.TextChannel
import org.slf4j.Logger


@Serializable
sealed class League {
    var isRunning: Boolean = false
    val sid: String = "yay"
    val picks: MutableMap<Long, MutableList<DraftPokemon>> = mutableMapOf()
    val battleorder: MutableMap<Int, String> = mutableMapOf()
    val results: MutableMap<String, Long> = mutableMapOf()
    val allowed: MutableMap<Long, Long> = mutableMapOf()
    private val mentions: Map<Long, Long> = emptyMap()
    val guild: Long = -1
    var round = 1
    var current = -1L
    private var cooldown = -1L
    val points: MutableMap<Long, Int> = mutableMapOf()

    private val originalorder: Map<Int, List<Int>> = mapOf()

    val order: MutableMap<Int, MutableList<Int>> = mutableMapOf()

    private val moved: MutableMap<Long, MutableList<Int>> = mutableMapOf()

    private val names: MutableMap<Long, String> = mutableMapOf()

    @Transient
    lateinit var tc: TextChannel

    private var tcid: Long = -1

    val tierlist: Tierlist by Tierlist.Delegate()
    val isPointBased
        get() = tierlist.isPointBased

    @Transient
    val timerScope = CoroutineScope(Dispatchers.Default + CoroutineExceptionHandler { _, t ->
        logger.error(
            "ERROR EXECUTING TIMER",
            t
        )
    })

    @Transient
    var cooldownJob: Job? = null
    val members: List<Long>
        get() = table
    private val timer: DraftTimer = DraftTimer.ASL
    val isLastRound: Boolean get() = round == tierlist.rounds

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


    open fun isCurrent(user: Long): Boolean {
        if (current == user || user == Constants.FLOID) return true
        return allowed[user] == current
    }

    fun isPicked(mon: String) = picks.values.any { l -> l.any { it.name.equals(mon, ignoreCase = true) } }

    fun handlePoints(e: GuildCommandEvent, needed: Int): Boolean {
        if (isPointBased) {
            if (points[current]!! - needed < 0) {
                e.reply("Dafür hast du nicht genug Punkte!")
                return true
            }
            if ((tierlist.rounds - (picks[current]!!.size + 1)) * tierlist.prices.values.min() > points[current]!! - needed) {
                e.reply("Wenn du dir dieses Pokemon holen würdest, kann dein Kader nicht mehr vervollständigt werden!")
                return true
            }
            points[current] = points[current]!! - needed
        }
        return false
    }


    fun nextPlayer() {
        if (endOfTurn()) return
        current = order[round]!!.nextCurrent()
        cooldownJob!!.cancel("Timer runs out")
        announcePlayer()
        restartTimer()
        saveEmolgaJSON()
    }

    private fun MutableList<Int>.nextCurrent() = table[this.removeAt(0)]

    suspend fun startDraft(tc: TextChannel?, fromFile: Boolean) {
        logger.info("Starting draft ${Emolga.get.drafts.reverseGet(this)}...")
        logger.info(tcid.toString())
        this.tc = tc ?: EmolgaMain.emolgajda.getTextChannelById(tcid)!!
        if (names.isEmpty()) names.putAll(
            this.tc.guild.retrieveMembersByIds(members).await().associate { it.idLong to it.effectiveName })
        logger.info(names.toString())
        if (tc != null)
            this.tcid = tc.idLong
        for (member in members) {
            if (fromFile) picks.putIfAbsent(member, mutableListOf())
            else picks[member] = mutableListOf()
            if (isPointBased) points[member] = tierlist.points - picks[member]!!.sumOf { tierlist.prices[it.tier]!! }
        }
        if (!fromFile) {
            order.clear()
            order.putAll(originalorder.mapValues { it.value.toMutableList() })
            current = order[1]!!.nextCurrent()
            restartTimer()
            sendRound()
            announcePlayer()
        } else {
            val delay = if (cooldown != -1L) cooldown - System.currentTimeMillis() else timer.calc()
            restartTimer(delay)
        }
        saveEmolgaJSON()
        isRunning = true
        logger.info("Started!")
    }

    private fun restartTimer(delay: Long = timer.calc()) {
        cooldown = System.currentTimeMillis() + delay
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

    fun getTierOf(args: Command.ArgumentManager, pokemon: String) = if (args.has("tier") && !isPointBased) {
        tierlist.order.firstOrNull { args.getText("tier").equals(it, ignoreCase = true) } ?: ""
    } else {
        tierlist.getTierOf(pokemon)
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
                saveEmolgaJSON()
                isRunning = false
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
        mentions[mem]?.let { "<@$it> (${getName(mem)})" }
        return "<@${mentions[mem] ?: mem}>"
    }

    private fun getName(mem: Long) = names[mem]!!

    fun isPickedBy(mon: String, mem: Long): Boolean = picks[mem]!!.any { it.name == mon }
    fun indexInRound(round: Int): Int = originalorder[round]!!.indexOf(current.indexedBy(table))
    fun triggerTimer(tr: TimerReason = TimerReason.REALTIMER) {
        triggerMove()
        if (endOfTurn()) return
        /*String msg = tr == TimerReason.REALTIMER ? "**" + current.getEffectiveName() + "** war zu langsam und deshalb ist jetzt " + getMention(order.get(round).get(0)) + " (<@&" + asl.getLongList("roleids").get(getIndex(order.get(round).get(0).getIdLong())) + ">) dran! "
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
        saveEmolgaJSON()
    }

    fun addFinished(mem: Long) {
        val index = mem.indexedBy(table)
        order.values.forEach { it.remove(index) }
    }

    abstract val docEntry: DocEntry?

    fun builder() = RequestBuilder(sid)
    suspend fun replyPick(e: GuildCommandEvent, pokemon: String, mem: Long) {
        e.slashCommandEvent!!.reply(
            "${e.member.asMention} hat${
                if (e.author.idLong != mem) " für **${getName(mem)}**" else ""
            } $pokemon gepickt!"
        ).await()
    }

    suspend fun replyRandomPick(e: GuildCommandEvent, pokemon: String, mem: Long, tier: String) {
        e.slashCommandEvent!!.reply(
            if (e.author.idLong != mem) "${e.member.asMention} hat für **${getName(mem)}** einen Random-Pick gemacht und **$pokemon** bekommen!" else "**<@${e.member.asMention}>** hat aus dem $tier-Tier ein **$pokemon** bekommen!"
        ).await()
    }

    companion object {
        val logger: Logger by SLF4J

        fun byChannel(e: GuildCommandEvent) = onlyChannel(e.textChannel)?.apply {
            if (!isCurrent(e.member.idLong)) {
                e.reply("Du bist nicht dran!")
                return null
            }
        }

        fun onlyChannel(tc: TextChannel) =
            Emolga.get.drafts.values.firstOrNull { it.isRunning && it.tc.idLong == tc.idLong }
    }

    enum class TimerReason {
        REALTIMER, SKIP
    }
}