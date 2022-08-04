package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import org.slf4j.LoggerFactory

class SpeedCommand : Command(
    "speed",
    "Zeigt die Init-Base und die maximale Initiative der pokemon auf Level 100 an.",
    CommandCategory.Pokemon
) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noSpecifiedArgs(
            "!speed <Pokemon1> <Pokemon2> usw.",
            "!speed Galvantula M-Gallade Primarene Bisaflor"
        )
    }

    override suspend fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        val m = e.message!!
        val msg = m.contentDisplay
        try {
            val s = msg.substring(7)
            val mons: List<String> = if (!s.contains("\n")) WHITESPACES_SPLITTER.split(s) else WNW.split(s)
            val speedMons = ArrayList<SpeedMon>()
            val datajson = dataJSON
            for (mon in mons) {
                logger.info("mon = $mon")
                var bs: Int
                var ger: String
                if (mon.startsWith("M-")) {
                    val st = getGerName(mon.substring(2))
                    if (st.isEmpty || !st.isFromType(Translation.Type.POKEMON)) {
                        tco.sendMessage(mon.substring(2) + " ist kein Pokemon!").queue()
                        return
                    }
                    ger = st.translation
                    bs = datajson.getJSONObject(getSDName(ger) + "mega").getJSONObject("baseStats").getInt("spe")
                } else if (mon.startsWith("A-")) {
                    val st = getGerName(mon.substring(2))
                    if (st.isEmpty || !st.isFromType(Translation.Type.POKEMON)) {
                        tco.sendMessage(mon.substring(2) + " ist kein Pokemon!").queue()
                        return
                    }
                    ger = st.translation
                    bs = datajson.getJSONObject(getSDName(ger) + "alola").getJSONObject("baseStats").getInt("spe")
                } else if (mon.startsWith("G-")) {
                    val st = getGerName(mon.substring(2))
                    if (st.isEmpty || !st.isFromType(Translation.Type.POKEMON)) {
                        tco.sendMessage(mon.substring(2) + " ist kein Pokemon!").queue()
                        return
                    }
                    ger = st.translation
                    bs = datajson.getJSONObject(getSDName(ger) + "galar").getJSONObject("baseStats").getInt("spe")
                } else {
                    if (mon.startsWith("Amigento") || mon.startsWith("Silvally")) {
                        speedMons.add(SpeedMon("Amigento", 95, 317))
                        continue
                    } else if (mon.startsWith("Rotom")) {
                        if (mon.equals("Rotom", ignoreCase = true)) speedMons.add(
                            SpeedMon(
                                "Rotom",
                                91,
                                309
                            )
                        ) else speedMons.add(SpeedMon(mon, 86, 298))
                        continue
                    }
                    val op =
                        sdex.keys.firstOrNull { anotherString: String? -> mon.equals(anotherString, ignoreCase = true) }
                    if (op != null) {
                        ger = op
                        val englname = getEnglName(ger.split("-")[0])
                        bs = datajson.getJSONObject(toSDName(englname + sdex[mon])).getJSONObject("baseStats")
                            .getInt("spe")
                    } else {
                        val t = getGerName(mon)
                        if (t.isEmpty || !t.isFromType(Translation.Type.POKEMON)) {
                            tco.sendMessage("$mon ist kein Pokemon!").queue()
                            return
                        }
                        ger = t.translation
                        bs = datajson.getJSONObject(getSDName(ger)).getJSONObject("baseStats").getInt("spe")
                    }
                }
                val speed = ((2 * bs + 99) * 1.1).toInt()
                var prefix = ""
                if (mon.startsWith("M-")) prefix = "M-" else if (mon.startsWith("A-")) prefix =
                    "A-" else if (mon.startsWith("G-")) prefix = "G-"
                speedMons.add(SpeedMon(prefix + ger, bs, speed))
            }
            speedMons.sort()
            tco.sendMessage(speedMons.joinToString("\n")).queue()
        } catch (ex: Exception) {
            ex.printStackTrace()
            tco.sendMessage("Es ist ein Fehler aufgetreten!").queue()
        }
    }

    private inner class SpeedMon(val monName: String, val baseSpeed: Int, val maxSpeed: Int) : Comparable<SpeedMon> {
        override fun toString(): String {
            return "$monName: $baseSpeed -> $maxSpeed"
        }

        override fun compareTo(other: SpeedMon): Int {
            return other.baseSpeed.compareTo(baseSpeed)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SpeedCommand::class.java)
        private val WNW = Regex("\\s*\n\\s*")
    }
}