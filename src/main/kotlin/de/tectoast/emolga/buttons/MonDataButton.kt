package de.tectoast.emolga.buttons

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.embedColor
import de.tectoast.emolga.commands.pokemon.DataCommand
import de.tectoast.jsolf.JSONObject
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.editMessage_
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import org.slf4j.LoggerFactory
import java.awt.Color

class MonDataButton : ButtonListener("mondata") {
    override suspend fun process(e: ButtonInteractionEvent, name: String) {
        logger.info("e.getMessageIdLong() = " + e.messageIdLong)
        val dt = Command.monDataButtons[e.messageIdLong]
        if (dt == null) {
            e.editMessage_(
                embed = Embed(
                    title = "Diese Mon-Data funktioniert nicht mehr, da seitdem der Bot neugestartet wurde!",
                    color = embedColor
                )
            ).queue()
            return
        }
        val mon = dt.getData(name)
        val builder = EmbedBuilder()
        builder.addField("Englisch", mon.getString("name"), true)
        builder.addField("Dex", mon.getInt("num").toString(), true)
        val gender: String = if (mon.has("genderRatio")) {
            val gen = mon.getJSONObject("genderRatio")
            (gen.getDouble("M") * 100).toString() + "% ♂ " + gen.getDouble("F") * 100 + "% ♀"
        } else if (mon.has("gender")) {
            if (mon.getString("gender") == "M") "100% ♂" else if (mon.getString("gender") == "F") "100% ♀" else "Unbekannt"
        } else "50% ♂ 50% ♀"
        builder.addField("Geschlecht", gender, true)
        //list.forEach(j -> logger.info(j.toString(4)));
        val monname = mon.getString("name")
        if (monname.equals("silvally", ignoreCase = true) || monname.equals("arceus", ignoreCase = true)) {
            builder.addField("Typen", "Normal", false)
        } else {
            logger.info(mon.toString())
            builder.addField(
                "Typen",
                mon.getJSONArray("types").toStringList()
                    .joinToString(" ") { if (it == "Psychic") "Psycho" else Command.getGerNameNoCheck(it) },
                false
            )
        }
        builder.addField("Größe", mon.getDouble("heightm").toString() + " m", true)
        builder.addField("Gewicht", mon.getDouble("weightkg").toString() + " kg", true)
        builder.addField("Eigruppe", mon.getJSONArray("eggGroups").toStringList().joinToString(", ") {
            Command.getGerNameNoCheck(
                "E_$it"
            )
        }, true)
        if (monname.equals("silvally", ignoreCase = true) || monname.equals("arceus", ignoreCase = true)) {
            builder.addField(
                "Fähigkeiten",
                if (monname.equals("silvally", ignoreCase = true)) "Alpha-System" else "Variabilität",
                false
            )
        } else {
            val o = mon.getJSONObject("abilities")
            val b = StringBuilder()
            if (o.has("0")) {
                b.append(Command.getGerNameNoCheck(o.getString("0"))).append("\n")
            }
            if (o.has("1")) {
                b.append(Command.getGerNameNoCheck(o.getString("1"))).append("\n")
            }
            if (o.has("H")) {
                b.append(Command.getGerNameNoCheck(o.getString("H"))).append(" (VF)")
            }
            builder.addField("Fähigkeiten", b.toString(), false)
        }
        if (monname.equals("silvally", ignoreCase = true) || monname.equals("arceus", ignoreCase = true)
        ) {
            builder.addField(
                if (monname.equals("silvally", ignoreCase = true)) "Amigento" else "Arceus",
                if (monname.equals("silvally", ignoreCase = true)) """
                    KP: 95
                    Atk: 95
                    Def: 95
                    SpAtk: 95
                    SpDef: 95
                    Init: 95
                    Summe: 570
                """.trimIndent() else """
                    KP: 120
                    Atk: 120
                    Def: 120
                    SpAtk: 120
                    SpDef: 120
                    Init: 120
                    Summe: 720
                """.trimIndent(),
                false
            )
        } else {
            val stats = mon.getJSONObject("baseStats")
            val kp = stats.getInt("hp")
            val atk = stats.getInt("atk")
            val def = stats.getInt("def")
            val spa = stats.getInt("spa")
            val spd = stats.getInt("spd")
            val spe = stats.getInt("spe")
            val str = """
                KP: $kp
                Atk: $atk
                Def: $def
                SpAtk: $spa
                SpDef: $spd
                Init: $spe
                Summe: ${kp + atk + def + spa + spd + spe}
                """.trimIndent()

            /*origname.put(toadd.toString(), obj);
                                if (stat.containsKey(str)) stat.get(str).add(toadd.toString());
                                else stat.put(str, new ArrayList<>(Collections.singletonList(toadd.toString())));*/
            val prevoInfo = DataCommand.getPrevoInfo(mon)
            if (prevoInfo.isNotEmpty()) {
                builder.addField("Erhaltbarkeit", prevoInfo, false)
            }
            builder.addField("Basestats", str, false)

            /*for (String s : stat.keySet().stream().sorted(Comparator.comparing(o -> stat.get(o).stream().mapToInt(str -> formeNames.indexOf(toSDName(origname.get(str).getString("name")))).min().orElse(0))).collect(Collectors.toList())) {
                                builder.addField(String.join(", ", stat.get(s)), stat.get(s).stream().map(origname::get).map(DataCommand::getPrevoInfo).collect(Collectors.joining("")) + s, true);
                            }*/
        }
        builder.setImage(Command.getGen5SpriteWithoutGoogle(mon))
        builder.setTitle(Command.getGerNameWithForm(monname))
        builder.setColor(Color.CYAN)
        val list = dt.list
        e.editMessageEmbeds(builder.build()).setActionRows(Command.getActionRows(list) { o: JSONObject ->
            if (mon.getString("name") == o.getString("name")) Button.primary(
                "mondata;" + Command.toSDName(o.getString("name")), Command.getGerNameWithForm(o.getString("name"))
            ) else Button.secondary(
                "mondata;" + Command.toSDName(o.getString("name")), Command.getGerNameWithForm(o.getString("name"))
            )
        }).queue()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MonDataButton::class.java)
    }
}