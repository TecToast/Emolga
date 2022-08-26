package de.tectoast.emolga.utils.draft

import de.tectoast.emolga.commands.DraftNamePreference
import de.tectoast.emolga.commands.toSDName
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.sql.managers.TranslationsManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

@Suppress("unused")
@Serializable
class Tierlist(val guild: Long) {
    /**
     * HashMap containing<br></br>Keys: Tiers<br></br>Values: Lists with the mons
     */
    @Transient
    val tierlist: MutableMap<String, MutableList<String>> = mutableMapOf()

    /**
     * The price for each tier
     */
    val prices: Map<String, Int> = mapOf()
    val freepicks: Map<String, Int> = mapOf()
    private val nexttiers: List<Int> = listOf()
    val namepreference: DraftNamePreference = DraftNamePreference.SINGLE_CHAR_BEFORE

    /**
     * List with all pokemon in the sheets tierlists, columns are separated by an "NEXT"
     */
    private val tiercolumns: MutableList<MutableList<String>> = mutableListOf()
    private val trashmons: List<String> = listOf()
    private val additionalMons: Map<String, List<String>> = mapOf()
    private val englishnames: List<String> = listOf()

    /**
     * the amount of rounds in the draft
     */
    val rounds: Int = 0
    val mode: TierlistMode = TierlistMode.POINTS
    val points = 0


    val order: List<String>
        get() = prices.keys.toList()


    /**
     * if this tierlist is pointbased
     */
    val isPointBased: Boolean
        get() = mode == TierlistMode.POINTS

    val freePicksAmount: Int get() = freepicks["#AMOUNT#"] ?: 0

    /**
     * the possible points for a player
     */


    val autoComplete: Set<String> by lazy {
        (tierlist.values.flatten() + englishnames).toSet()
    }

    val pickableNicknames: Set<String> by lazy {
        TranslationsManager.getAllMonNicks().flatMap { mon ->
            tierlist.values.flatten().filter { it.substringAfter("-") == mon.value }
                .map { it.replace(mon.value, mon.key) }
        }.toSet()
    }

    init {
        require(!(rounds == -1 && mode != TierlistMode.NOTHING)) { "BRUDER OLF IST DAS DEIN SCHEIß ERNST" }
        val currtierlist: MutableList<String> = mutableListOf()
        var currtier = 0
        for ((x, monss) in tiercolumns.withIndex()) {
            val mon = monss.map { it.trim() }
                .map { REPLACE_NONSENSE.replace(it, "") }
            if (nexttiers.contains(x)) {
                val key = order[currtier++]
                tierlist[key] = ArrayList(currtierlist)
                currtierlist.clear()
            }
            currtierlist.addAll(mon)
        }
        tierlist[order[currtier]] = ArrayList(currtierlist)
        if (trashmons.isNotEmpty()) tierlist[order.last()]!!.addAll(trashmons)
        if (additionalMons.isNotEmpty()) {
            additionalMons.keys.forEach { tierlist[it]!!.addAll(additionalMons[it]!!) }
        }
        tierlists[this.guild] = this
    }


    fun getPointsNeeded(s: String): Int = when (mode) {
        TierlistMode.POINTS -> prices[getTierOf(s)] ?: -1
        TierlistMode.MIX -> freepicks[getTierOf(s)] ?: -1
        else -> -1
    }

    fun getTierOf(s: String): String {
        return tierlist.entries.firstOrNull { e -> e.value.any { s.toSDName() == it.toSDName() } }?.key ?: ""
    }

    private fun setupTiercolumns(
        mons: List<List<String>>,
        nexttiers: List<Int>,
        tiercols: MutableList<List<String>>,
        normal: Boolean
    ) {
        var currtier = 0
        val currtierlist: MutableList<String> = LinkedList()
        for ((x, monss) in mons.withIndex()) {
            val mon = monss.map { it.trim() }
                .map { REPLACE_NONSENSE.replace(it, "") }
            if (normal) {
                if (nexttiers.contains(x)) {
                    val key = order[currtier++]
                    tierlist[key] = ArrayList(currtierlist)
                    currtierlist.clear()
                }
                currtierlist.addAll(mon)
            }
            tiercols.add(mon)
        }
        if (normal) tierlist[order[currtier]] = ArrayList(currtierlist)
        tiercolumns.removeLast()
    }

    fun getNameOf(mon: String): String? {
        return tierlist.values.flatten().firstOrNull { it.equals(mon, ignoreCase = true) }
    }

    companion object : ReadOnlyProperty<League, Tierlist> {
        /**
         * All tierlists
         */
        val tierlists: MutableMap<Long, Tierlist> = HashMap()
        private val REPLACE_NONSENSE = Regex("[^a-zA-Z\\d-:%ßäöüÄÖÜé ]")
        fun setup() {
            tierlists.clear()
            val dir = File("./Tierlists/")
            for (file in dir.listFiles()!!) {
                if (file.isFile) /*Tierlist(file.name.substringBefore(".").toLong())*/ Json.decodeFromString<Tierlist>(
                    file.readText()
                )
            }
        }

        fun getByGuild(guild: String): Tierlist? {
            return getByGuild(guild.toLong())
        }


        fun getByGuild(guild: Long): Tierlist? {
            return tierlists[guild]
        }

        fun getLocation(mon: String?, defX: Int, defY: Int, tiercolumns: List<String>): Coord {
            var x = defX
            var y = defY
            var valid = false
            for (s in tiercolumns) {
                if (s.equals(mon, ignoreCase = true)) {
                    valid = true
                    break
                }
                //logger.info(s + " " + y);
                if (s == "NEXT") {
                    x++
                    y = defY
                } else y++
            }
            return Coord(x, y, valid)
        }

        override fun getValue(thisRef: League, property: KProperty<*>): Tierlist {
            return getByGuild(thisRef.guild)!!
        }
    }
}

@Suppress("unused")
enum class TierlistMode {
    POINTS,
    TIERS,
    MIX,
    NOTHING;

    fun isPoints() = this == POINTS
    fun isTiers() = this == TIERS
    fun isMix() = this == MIX
}