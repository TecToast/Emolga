package de.tectoast.emolga.commands.dexquiz

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.DexQuiz
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.interactions.components.buttons.Button
import java.awt.Color

class SolutionCommand : Command("solution", "Zeigt die Lösung des derzeitigen Eintrags", CommandCategory.Dexquiz) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        slash(true, 918865966136455249L, Constants.FPLID)
    }

    override fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        val quiz = DexQuiz.getByTC(tco)
        if (quiz != null) {
            e.slashCommandEvent!!.replyEmbeds(
                EmbedBuilder()
                    .setTitle("${e.member.effectiveName} hat vorgeschlagen, die Lösung aufzudecken!")
                    .setDescription("Wenn eine weitere Person auf den Button drückt, wird die Lösung aufgedeckt!")
                    .setColor(Color.CYAN)
                    .build()
            )
                .addActionRow(
                    Button.primary(
                        "solution;${tco.idLong}###${e.author.idLong}###${quiz.round}",
                        "Lösung aufdecken"
                    )
                )
                .queue()
        } else {
            tco.sendMessage("In diesem Channel wurde kein Dexquiz erstellt!").queue()
        }
    }
}