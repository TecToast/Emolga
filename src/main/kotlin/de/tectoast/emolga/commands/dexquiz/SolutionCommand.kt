package de.tectoast.emolga.commands.dexquiz

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.embedColor
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.DexQuiz
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.interactions.components.buttons.Button

class SolutionCommand : Command("solution", "Zeigt die Lösung des derzeitigen Eintrags", CommandCategory.Dexquiz) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        slash(true, 918865966136455249L, Constants.FPLID, Constants.CULTID)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        //val quiz = DexQuiz.getByTC(tco)
        DexQuiz.getByTC(tco)?.let { quiz ->
            e.slashCommandEvent!!.reply_(
                embed = Embed(
                    title = "${e.member.effectiveName} hat vorgeschlagen, die Lösung aufzudecken!",
                    description = "Wenn eine weitere Person auf den Button drückt, wird die Lösung aufgedeckt!",
                    color = embedColor
                ), components = Button.primary(
                    "solution;${tco.idLong}###${e.author.idLong}###${quiz.round}", "Lösung aufdecken"
                ).into()
            ).queue()
        } ?: e.reply("In diesem Channel wurde kein Dexquiz erstellt!")
    }
}