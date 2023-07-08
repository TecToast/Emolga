package de.tectoast.emolga.utils

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.dexquiz.DexQuizTip
import de.tectoast.emolga.database.exposed.PokedexDB
import de.tectoast.emolga.utils.records.DexEntry
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import java.awt.Color
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.*
import java.util.concurrent.TimeUnit

@Suppress("unused")
class DexQuiz(tc: GuildMessageChannel, rounds: Long) {
    private val totalRounds: Long
    private val points: MutableMap<Long, Int> = HashMap()
    private val tc: GuildMessageChannel
    lateinit var currentGerName: String
        private set
    lateinit var currentEnglName: String
        private set
    lateinit var currentEdition: String
        private set
    private val tipPoints: MutableMap<Long, Long> = HashMap()
    private var block = false
    private val random = Random()
    var round: Long = 1
        private set
    private val totalbudget: Long

    init {
        this.tc = tc
        totalRounds = rounds
        if (totalRounds <= 0) {
            tc.sendMessage("Du musst eine Mindestanzahl von einer Runde angeben!").queue()
        } else {
            activeQuizzes[tc.idLong] = this
        }
        val gid: Long = tc.guild.idLong
        totalbudget = ConfigManager.DEXQUIZ.getValue(gid, "totalbudget") as Int * totalRounds
        val b: EmbedBuilder = EmbedBuilder()
            .setTitle("Mögliche Tipps")
            .setDescription("Alle möglichen Tipps mit Preisen aufgelistet, konfigurierbar mit `/configurate dexquiz`")
            .setColor(Color.CYAN)
            .addField("Punkte-Budget", totalbudget.toString(), false)
        DexQuizTip.buildEmbedFields(gid)
            .forEach { field: MessageEmbed.Field -> b.addField(field) }
        tc.sendMessageEmbeds(b.build()).queue()
        newMon(false)
    }

    fun useTip(user: Long, tipName: String?): Long {
        val gid: Long = tc.guild.idLong
        val price = ConfigManager.DEXQUIZ.getValue(gid, tipName!!) as Int
        if (price == -1) return -10
        if (!tipPoints.containsKey(user)) tipPoints[user] = totalbudget
        return if (tipPoints[user]!! - price < 0) -1 else tipPoints.compute(user)
        { _: Long, i: Long? -> i!! - price }!!
    }

    private val newMon: DexPokemon
        get() {
            try {
                if (cachedMons == null) {
                    val file = File("./entwicklung.txt")
                    cachedMons = Files.readAllLines(file.toPath())
                }
                val pokemon = cachedMons!![random.nextInt(cachedMons!!.size)]
                val englName = Command.getEnglName(pokemon)
                return DexPokemon(pokemon, englName)
            } catch (e: IOException) {
                throw e
            }
        }

    fun check(t: String): Boolean {
        return t.equals(currentGerName, ignoreCase = true) || t.equals(currentEnglName, ignoreCase = true)
    }

    fun end() {
        activeQuizzes.remove(tc.idLong)
        if (points.isEmpty() && round > 1) {
            tc.sendMessage("Nur den Solution Command zu benutzen ist nicht der Sinn der Sache! xD").queue()
            return
        }
        tc.sendMessage(buildString {
            append("Punkte:\n")
            points.keys.sortedByDescending { points[it] }.forEach { append("<@$it>: ${points[it]}\n") }
        }).queue()
    }

    fun nextRound() {
        incrementRound()
        if (isEnded) {
            end()
            return
        }
        newMon()
    }


    private fun newMon(withDelay: Boolean = true) {
        val mon = newMon
        val pokemon: String = mon.germanName
        val englName: String = mon.englName
        val dexEntry: DexEntry = PokedexDB.getDexEntry(pokemon)
        val entry: String = dexEntry.entry
        currentEdition = dexEntry.edition
        currentGerName = pokemon
        Command.sendDexEntry(tc.asMention + " " + pokemon)
        currentEnglName = englName
        //ü = %C3%B6
        block = false
        val ma = tc.sendMessage(
            "Runde $round/$totalRounds: ${Command.trim(entry, pokemon)}\nZu welchem Pokemon gehört dieser Dex-Eintrag?"
        )
        if (withDelay) ma.queueAfter(3, TimeUnit.SECONDS) else ma.queue()
    }

    fun givePoint(member: Long) {
        points.compute(
            member
        )
        { _: Long, i: Int? -> if (i == null) 1 else i + 1 }
    }

    private val isEnded: Boolean
        get() = round > totalRounds

    fun nonBlocking(): Boolean {
        return !block
    }

    fun block() {
        block = true
    }

    private fun incrementRound() {
        round++
    }

    class DexPokemon(val germanName: String, val englName: String)
    companion object {
        private val activeQuizzes: MutableMap<Long, DexQuiz> = HashMap()
        private var cachedMons: List<String>? = null
        fun getByTC(tc: GuildMessageChannel): DexQuiz? {
            return getByTC(tc.idLong)
        }

        fun getByTC(tcid: Long): DexQuiz? {
            return activeQuizzes[tcid]
        }
    }
}
