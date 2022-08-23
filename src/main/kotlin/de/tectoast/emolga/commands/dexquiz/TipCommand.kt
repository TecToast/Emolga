package de.tectoast.emolga.commands.dexquiz

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.dexquiz.DexQuizTip.TipData
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.DexQuiz

class TipCommand : Command("tip", "Zeigt einen Tipp für den derzeitigen Eintrag", CommandCategory.Dexquiz) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "tip", "Tipp", "Der Tipp, den du haben möchtest", ArgumentManagerTemplate.Text.of(
                    DexQuizTip.buildSubcommands(), true
                )
            )
            .setExample("/tip anfangsbuchstabe")
            .build()
        slash(true, 918865966136455249L, Constants.G.FPL, Constants.G.CULT)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        val quiz = DexQuiz.getByTC(tco)
        if (quiz != null) {
            val tip: String = e.arguments.getText("tip").uppercase()
            val newBudget = quiz.useTip(e.author.idLong, tip)
            if (newBudget == -10L) {
                e.reply(
                    "Dieser Tipp ist auf diesem Server zurzeit deaktiviert! Nutze `/configurate dexquiz` um dies zu ändern!",
                    true
                )
                return
            }
            if (newBudget < 0) {
                e.reply("Dafür hast du nicht mehr genug Budget!", true)
                return
            }
            e.reply(
                DexQuizTip.valueOf(tip).tipFunction(
                    TipData(
                        quiz.currentGerName, quiz.currentEnglName, quiz.currentEdition,
                        getDataObject(quiz.currentGerName)
                    )
                ) + "\nNeuer Kontostand: **$newBudget**", true
            )
        } else {
            e.reply("In diesem Channel wurde kein Dexquiz erstellt!", true)
        }
    }
}