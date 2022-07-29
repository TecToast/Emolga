package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import net.dv8tion.jda.api.EmbedBuilder
import org.slf4j.LoggerFactory
import java.awt.Color

class MovesCommand : Command(
    "moves",
    "Zeigt die möglichen Attacken des pokemon an (Entweder alle oder nur die physischen etc.)",
    CommandCategory.Pokemon
) {
    init {
        aliases.add("moves5")
        argumentTemplate = ArgumentManagerTemplate.builder().add(
            "form", "Form", "Optionale alternative Form", ArgumentManagerTemplate.Text.of(
                SubCommand.of("Alola"), SubCommand.of("Galar")
            ), true
        ).add("mon", "Pokemon", "Das Pokemon", Translation.Type.POKEMON).setExample("!moves Emolga --spez --flying")
            .setCustomDescription("!moves [Alola|Galar] <Pokemon> [--phys|--spez|--status] [--feuer|...]")
            .setNoCheck(true).build()
    }

    override fun process(e: GuildCommandEvent) {
        val m = e.message!!
        val msg = m.contentDisplay
        val args = e.arguments

        /*var pokemon: String
        var form = "Normal"
        if (args[1].lowercase().contains("alola")) {
            pokemon = args[2]
            form = "Alola"
        } else if (args[1].lowercase().contains("galar")) {
            pokemon = args[2]
            form = "Galar"
        } else {
            pokemon = args[1]
        }
        val t = getGerName(pokemon, false)
        if (!t.isFromType(Translation.Type.POKEMON)) {
            tco.sendMessage("Das ist kein Pokemon!").queue()
            return
        }
        pokemon = t.translation*/
        val pokemon = args.getTranslation("mon").translation
        val form = args.getOrDefault("form", "Normal")
        logger.info(pokemon)
        val attacks: List<String>
        val gen = if (e.guild.id == "747357029714231299" || e.usedName.equals("!moves5", ignoreCase = true)) 5 else 8
        //logger.info("args[0] = " + args[0]);
        //logger.info("gen = " + gen);
        attacks = getAttacksFrom(pokemon, msg, form, gen).sorted()
        if (attacks.isEmpty()) {
            e.reply("$pokemon kann keine Attacken mit den angegebenen Spezifikationen erlernen!")

        } else {
            if (attacks.size == 1) if (attacks[0] == "ERROR") {
                e.reply("Dieses pokemon hat keine $form-Form!")
                return
            }
            var builder = EmbedBuilder()
            val prefix = if (form == "Normal") "" else "$form-"
            builder.setTitle("Attacken von $prefix$pokemon").setColor(Color.CYAN)
            var str = StringBuilder()
            for (o in attacks) {
                str.append(o).append("\n")
                if (str.length > 1900) {
                    e.reply(builder.setDescription(str.toString()).build())
                    builder = EmbedBuilder()
                    builder.setTitle("Attacken von $prefix$pokemon").setColor(Color.CYAN)
                    str = StringBuilder()
                }
            }
            builder.setDescription(str.toString())
            e.reply(builder.build())
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MovesCommand::class.java)
    }
}