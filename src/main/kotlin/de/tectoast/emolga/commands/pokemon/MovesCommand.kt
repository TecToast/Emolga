package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import net.dv8tion.jda.api.EmbedBuilder
import org.slf4j.LoggerFactory
import java.awt.Color
import java.util.*

class MovesCommand : Command(
    "moves",
    "Zeigt die möglichen Attacken des pokemon an (Entweder alle oder nur die physischen etc.)",
    CommandCategory.Pokemon
) {
    init {
        aliases.add("moves5")
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "form", "Form", "Optionale alternative Form", ArgumentManagerTemplate.Text.of(
                    SubCommand.of("Alola"), SubCommand.of("Galar")
                ), true
            )
            .add("mon", "Pokemon", "Das Pokemon", Translation.Type.POKEMON)
            .setExample("!moves Emolga --spez --flying")
            .setCustomDescription("!moves [Alola|Galar] <Pokemon> [--phys|--spez|--status] [--feuer|...]")
            .setNoCheck(true)
            .build()
    }

    override fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        val m = e.message!!
        val msg = m.contentDisplay
        val args = msg.split(" ".toRegex())
        if (args.size > 1) {
            var pokemon: String
            var form = "Normal"
            if (args[1].lowercase(Locale.getDefault()).contains("alola")) {
                pokemon = args[2]
                form = "Alola"
            } else if (args[1].lowercase(Locale.getDefault()).contains("galar")) {
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
            pokemon = t.translation
            logger.info(pokemon)
            try {
                val attacks: List<String>
                val gen =
                    if (e.guild.id == "747357029714231299" || args[0].equals("!moves5", ignoreCase = true)) 5 else 8
                //logger.info("args[0] = " + args[0]);
                //logger.info("gen = " + gen);
                attacks = getAttacksFrom(pokemon, msg, form, gen).sorted()
                if (attacks.isEmpty()) {
                    tco.sendMessage("$pokemon kann keine Attacken mit den angegebenen Spezifikationen erlernen!")
                        .queue()
                } else {
                    if (attacks.size == 1) if (attacks[0] == "ERROR") {
                        tco.sendMessage("Dieses pokemon hat keine $form-Form!").queue()
                        return
                    }
                    var builder = EmbedBuilder()
                    val prefix = if (form == "Normal") "" else "$form-"
                    builder.setTitle("Attacken von $prefix$pokemon").setColor(Color.CYAN)
                    var str = StringBuilder()
                    for (o in attacks) {
                        str.append(o).append("\n")
                        if (str.length > 1900) {
                            tco.sendMessageEmbeds(builder.setDescription(str.toString()).build()).queue()
                            builder = EmbedBuilder()
                            builder.setTitle("Attacken von $prefix$pokemon").setColor(Color.CYAN)
                            str = StringBuilder()
                        }
                    }
                    builder.setDescription(str.toString())
                    tco.sendMessageEmbeds(builder.build()).queue()
                }
            } catch (ioException: Exception) {
                tco.sendMessage("Es ist ein Fehler aufgetreten!").queue()
                ioException.printStackTrace()
            }
        } else tco.sendMessage("Syntax: !moves <pokemon>").queue()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MovesCommand::class.java)
    }
}