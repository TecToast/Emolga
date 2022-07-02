package de.tectoast.emolga.commands.dexquiz

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.DexQuiz

class DexquizCommand :
    Command("dexquiz", "Erstellt ein Dexquiz mit der angegebenen Anzahl an Einträgen", CommandCategory.Dexquiz) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        slash(true, 918865966136455249L, Constants.FPLID)
    }

    override fun process(e: GuildCommandEvent) {}
    class Start : Command("start", "Startet ein Dexquiz") {
        init {
            argumentTemplate = ArgumentManagerTemplate.builder()
                .add(
                    "count",
                    "Rundenanzahl",
                    "Die Anzahl an Runden, die du spielen möchtest",
                    ArgumentManagerTemplate.DiscordType.INTEGER
                )
                .build()
        }

        override fun process(e: GuildCommandEvent) {
            val tco = e.textChannel
            val quiz = DexQuiz.getByTC(tco)
            if (quiz != null) {
                e.reply("In diesem Channel läuft bereits ein Dexquiz! Wenn du dieses beenden möchtest, verwende `/dexquiz end`.")
                return
            }
            try {
                DexQuiz(tco, e.arguments!!.getLong("count"))
                if (e.isSlash) e.slashCommandEvent!!.reply("\uD83D\uDC4D").setEphemeral(true).queue()
            } catch (ioException: Exception) {
                tco.sendMessage("Es ist ein Fehler aufgetreten!").queue()
                ioException.printStackTrace()
            }
        }
    }

    class End : Command("end", "Beendet das Dexquiz in diesem Channel") {
        init {
            argumentTemplate = ArgumentManagerTemplate.noArgs()
        }

        override fun process(e: GuildCommandEvent) {
            val tco = e.textChannel
            val quiz = DexQuiz.getByTC(tco)
            if (quiz != null) {
                e.reply("Die Lösung des alten Quizzes war " + quiz.currentGerName + "!")
                quiz.end()
            }
        }
    }
}