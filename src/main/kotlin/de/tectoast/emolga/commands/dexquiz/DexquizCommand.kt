package de.tectoast.emolga.commands.dexquiz

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.DexQuiz
import dev.minn.jda.ktx.messages.reply_

class DexquizCommand :
    Command("dexquiz", "Erstellt ein Dexquiz mit der angegebenen Anzahl an Einträgen", CommandCategory.Dexquiz) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        slash(true, 918865966136455249L, Constants.G.FPL, Constants.G.CULT)
    }

    override suspend fun process(e: GuildCommandEvent) {}
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

        override suspend fun process(e: GuildCommandEvent) {
            val tco = e.textChannel
            if (DexQuiz.getByTC(tco) != null) {
                e.reply("In diesem Channel läuft bereits ein Dexquiz! Wenn du dieses beenden möchtest, verwende `/dexquiz end`.")
                return
            }
            try {
                DexQuiz(tco, e.arguments.getLong("count"))
                e.slashCommandEvent?.reply_("\uD83D\uDC4D", ephemeral = true)?.queue()
            } catch (ex: Exception) {
                tco.sendMessage("Es ist ein Fehler aufgetreten!").queue()
                ex.printStackTrace()
            }
        }
    }

    class End : Command("end", "Beendet das Dexquiz in diesem Channel") {
        init {
            argumentTemplate = ArgumentManagerTemplate.noArgs()
        }

        override suspend fun process(e: GuildCommandEvent) {
            DexQuiz.getByTC(e.textChannel)?.let {
                e.reply("Die Lösung des alten Quizzes war " + it.currentGerName + "!")
                it.end()
            } ?: e.reply("In diesem Channel findet zurzeit kein Dexquiz statt!")
        }
    }
}