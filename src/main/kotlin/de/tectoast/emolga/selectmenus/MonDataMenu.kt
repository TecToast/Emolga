package de.tectoast.emolga.selectmenus

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.condAppend
import de.tectoast.emolga.commands.pokemon.DataCommand.Companion.getPrevoInfo
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import org.slf4j.LoggerFactory
import java.awt.Color

class MonDataMenu : MenuListener("mondata") {
    override suspend fun process(e: StringSelectInteractionEvent, menuname: String?) {
        /*e.reply("Dieses Menü funktioniert noch nicht, aber Flo arbeitet zurzeit daran :3").setEphemeral(true).queue();
        if (true) return;*/
        logger.info("e.getMessageIdLong() = " + e.messageIdLong)
        val name = e.values[0]
        /*MonData dt = monDataButtons.get(e.getMessageIdLong());
        if (dt == null) {
            e.editMessageEmbeds(new EmbedBuilder().setTitle("Ach Mensch " + e.getMember().getEffectiveName() + ", diese Mon-Data funktioniert nicht mehr, da seitdem der Bot neugestartet wurde!").setColor(Color.CYAN).build()).queue();
            return;
        }*/
        val mon = Command.dataJSON[name]!!
        val builder = EmbedBuilder()
        builder.addField("Englisch", mon.name, true)
        builder.addField("Dex", mon.num.toString(), true)
        builder.addField(
            "Geschlecht",
            mon.genderRatio?.let { "${it["M"]!! * 100}% ♂ ${it["F"]!! * 100}% ♀" }
                ?: mon.gender?.let { if (it == "M") "100% ♂" else "100% ♀" } ?: "50% ♂ 50% ♀",
            true)
        //list.forEach(j -> logger.info(j.toString(4)));
        val monname = mon.name
        if (monname.equals("silvally", ignoreCase = true) || monname.equals("arceus", ignoreCase = true)) {
            builder.addField("Typen", "Normal", false)
        } else {
            builder.addField("Typen", mon.types.joinToString(" ") {
                if (it == "Psychic") "Psycho"
                else Command.getGerNameNoCheck(it)
            }, false)
        }
        builder.addField("Größe", "${mon.heightm} m", true)
        builder.addField("Gewicht", "${mon.weightkg} kg", true)
        builder.addField("Eigruppe", mon.eggGroups.joinToString {
            Command.getGerNameNoCheck("E_$it")
        }, true)
        if (monname.equals("silvally", ignoreCase = true) || monname.equals("arceus", ignoreCase = true)) {
            builder.addField(
                "Fähigkeiten",
                if (monname.equals("silvally", ignoreCase = true)) "Alpha-System" else "Variabilität",
                false
            )
        } else {
            builder.addField("Fähigkeiten", mon.abilities.entries.joinToString("\n") {
                Command.getGerNameNoCheck(it.value).condAppend(it.key == "H", " (VF)")
            }, false)
        }
        if (monname.equals("silvally", ignoreCase = true) || monname.equals("arceus", ignoreCase = true)) {
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
            val str = mon.buildStatString()
            val prevoInfo = getPrevoInfo(mon)
            if (prevoInfo.isNotEmpty()) {
                builder.addField("Erhaltbarkeit", prevoInfo, false)
            }
            builder.addField("Basestats", str, false)
        }
        builder.setImage(Command.getGen5SpriteWithoutGoogle(mon))
        builder.setTitle(Command.getGerNameWithForm(monname))
        builder.setColor(Color.CYAN)
        e.editMessageEmbeds(builder.build())
            .setActionRow(StringSelectMenu.create("mondata").addOptions(e.selectMenu.options.map { o: SelectOption ->
                o.withDefault(
                    o.value == name
                )
            }).build()).queue()
        //e.getHook().editOriginalEmbeds(builder.build()).queue();
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MonDataMenu::class.java)
    }
}
