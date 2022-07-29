package de.tectoast.emolga.buttons.buttonsaves

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.stream.Collectors

class TrainerData(trainerName: String) {
    val mons = LinkedHashMap<String, List<TrainerMon>>()
    var current: String? = null
    var isWithMoveset = false
        private set

    init {
        val d: Document
        try {
            d = Jsoup.connect("https://www.pokewiki.de/$trainerName").get()
            val elelist = d.select("table[class=\"lightBg1 round darkBorder1\"]")
            for (elele in elelist) {
                val ele = elele.child(0).child(1)
                val set = elele.child(0).child(0).text()
                val list: MutableList<TrainerMon> = mutableListOf()
                for (child in ele.children()) {
                    val t = child.child(0).child(0)
                    val itm = t.child(t.children().size - 6).text().trim()
                    val moves: MutableList<String> = mutableListOf()
                    for (i in 7..10) {
                        t.child(i - (11 - t.children().size)).child(0).text().trim().takeUnless { it == "—" }
                            ?.let { moves.add(it) }
                    }
                    val alt = t.child(0).child(0).child(0).attr("alt")
                    val monname = if (alt.startsWith("Sugimori")) t.child(1).text() else alt
                    list.add(
                        TrainerMon(
                            monname,
                            t.child(2).child(0).child(0).text().substring(4),
                            if (t.children().size == 11) t.child(4).text() else null,
                            if (!itm.contains("Kein Item")) itm else null,
                            moves
                        )
                    )
                }
                mons[set.replace("*", "").trim()] = list
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun swapWithMoveset() {
        isWithMoveset = !isWithMoveset
    }

    fun isCurrent(fight: String?): Boolean {
        return current == fight
    }

    val monsList: Collection<String>
        get() = mons.keys

    fun getMonsFrom(set: String, withMoveset: Boolean): String {
        return mons[getNormalName(set)]!!.joinToString("\n\n") { it.build(!withMoveset) }
    }

    fun getNormalName(name: String): String? {
        logger.info("getNormalName name = {}", name)
        val s = mons.keys.firstOrNull { it.equals(name, ignoreCase = true) }
        logger.info("s = {}", s)
        return s
    }

    inner class TrainerMon(
        val name: String,
        private val level: String,
        val ability: String?,
        val item: String?,
        val moves: List<String>,
    ) {
        fun build(onlyWithLevel: Boolean): String {
            return if (onlyWithLevel) "$name (Level $level)" else buildString {
                append(name)
                append((if (item != null && item.trim().isNotEmpty()) " @ $item" else ""))
                append("\n")
                append((if (ability != null) "Fähigkeit: $ability\n" else ""))
                append("Level: ")
                append(level)
                append("\n")
                append(moves.stream().map { "- $it" }.collect(Collectors.joining("\n")))
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TrainerData::class.java)
    }
}