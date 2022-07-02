package de.tectoast.emolga.utils.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.utils.records.Coord
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*
import java.util.function.Consumer
import java.util.regex.Pattern
import java.util.stream.Collectors
import kotlin.reflect.KProperty

class Tierlist(guild: String) {
    /**
     * HashMap containing<br></br>Keys: Tiers<br></br>Values: Lists with the mons
     */
    val tierlist: MutableMap<String, MutableList<String>> = LinkedHashMap()

    /**
     * The price for each tier
     */
    val prices: MutableMap<String?, Int?> = HashMap()

    /**
     * The guild of this tierlist
     */
    val guild: Long

    /**
     * List with all pokemon in the sheets tierlists, columns are separated by an "NEXT"
     */
    val tiercolumns = LinkedList<String>()

    /**
     * List with all pokemon in the sheets tierlists, columns are separated by an "NEXT" SORTED BY ENGLISCH NAMES
     */
    val tiercolumnsEngl = LinkedList<String>()

    /**
     * Order of the tiers, from highest to lowest
     */
    @JvmField
    val order: MutableList<String> = ArrayList()

    /**
     * the amount of rounds in the draft
     */
    var rounds: Int

    /**
     * if this tierlist is pointbased
     */
    var isPointBased = false

    /**
     * the possible points for a player
     */
    var points = 0

    init {
        this.guild = guild.substring(0, guild.length - 5).toLong()
        val o = Command.load("./Tierlists/$guild")
        if (!o.has("mode")) {
            throw IllegalStateException("Mode not set")
        }
        rounds = o.optInt("rounds", -1)
        val mode = o.getString("mode")
        require(!(rounds == -1 && mode != "nothing")) { "BRUDER OLF IST DAS DEIN SCHEIß ERNST" }
        when (mode) {
            "points" -> {
                points = o.getInt("points")
                isPointBased = true
            }
            "tiers", "nothing" -> isPointBased = false
            else -> throw IllegalArgumentException("Invalid mode! Has to be one of 'points', 'tiers' or 'nothing'!")
        }
        val tiers = o.getJSONObject("tiers")
        for (s in tiers.keySet()) {
            order.add(s)
            prices[s] = tiers.getInt(s)
        }
        setupTiercolumns(
            o.getJSONArray("mons").toListList(String::class.java),
            o.getIntList("nexttiers"),
            tiercolumns,
            true
        )
        if (o.has("monsengl")) setupTiercolumns(
            o.getJSONArray("monsengl").toListList(String::class.java),
            o.getIntList("nexttiers"),
            tiercolumnsEngl,
            false
        )
        if (o.has("trashmons")) tierlist[order[order.size - 1]]!!.addAll(o.getStringList("trashmons"))
        if (o.has("additionalmons")) {
            val am = o.getJSONObject("additionalmons")
            am.keySet().forEach(Consumer { k: String -> tierlist[k]!!.addAll(am.getStringList(k)) })
        }
        tierlists[this.guild] = this
    }

    fun removeMon(mon: String) {
        tierlist.values.forEach(Consumer { l: MutableList<String> ->
            l.removeIf { anotherString: String? ->
                mon.equals(
                    anotherString,
                    ignoreCase = true
                )
            }
        })
    }

    fun getPointsNeeded(s: String): Int {
        for ((key, value) in tierlist) {
            if (value.stream()
                    .anyMatch { anotherString: String? -> s.equals(anotherString, ignoreCase = true) }
            ) return prices[key]!!
        }
        return -1
    }

    fun getTierOf(s: String): String {
        for ((key, value) in tierlist) {
            if (value.stream().anyMatch { str: String -> Command.toSDName(str) == Command.toSDName(s) }) return key
        }
        return ""
    }

    private fun setupTiercolumns(
        mons: List<List<String>>,
        nexttiers: List<Int>,
        tiercols: MutableList<String>,
        normal: Boolean
    ) {
        var currtier = 0
        val currtierlist: MutableList<String> = LinkedList()
        for ((x, monss) in mons.withIndex()) {
            val mon = monss.stream().map { obj: String -> obj.trim() }
                .map { s: String? -> REPLACE_NONSENSE.matcher(s).replaceAll("") }
                .toList()
            if (normal) {
                if (nexttiers.contains(x)) {
                    val key = order[currtier++]
                    tierlist[key] = ArrayList(currtierlist)
                    currtierlist.clear()
                }
                currtierlist.addAll(mon)
            }
            tiercols.addAll(mon)
            tiercols.add("NEXT")
        }
        if (normal) tierlist[order[currtier]] = ArrayList(currtierlist)
        tiercolumns.removeLast()
    }

    fun getLocation(mon: String?): Coord {
        return getLocation(mon, 0, 0)
    }

    fun getLocation(mon: String?, defX: Int, defY: Int): Coord {
        return getLocation(mon, defX, defY, tiercolumns)
    }

    fun getNameOf(s: String): String {
        for ((_, value) in tierlist) {
            val str = value.stream().filter { anotherString: String? -> s.equals(anotherString, ignoreCase = true) }
                .collect(Collectors.joining(""))
            if (str.isNotEmpty()) return str
        }
        return ""
    }

    class Delegate() {
        operator fun getValue(thisRef: Draft, property: KProperty<*>): Tierlist {
            return getByGuild(thisRef.guild)!!
        }
    }

    companion object {
        /**
         * All tierlists
         */
        val tierlists: MutableMap<Long, Tierlist> = HashMap()
        private val logger = LoggerFactory.getLogger(Tierlist::class.java)
        private val REPLACE_NONSENSE = Pattern.compile("[^a-zA-Z\\d-:% ]")
        fun setup() {
            tierlists.clear()
            val dir = File("./Tierlists/")
            for (file in dir.listFiles()!!) {
                if (file.isFile) Tierlist(file.name)
            }
        }

        fun getByGuild(guild: String): Tierlist? {
            return getByGuild(guild.toLong())
        }

        @JvmStatic
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
    }
}