package de.tectoast.emolga.buttons

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.utils.DexQuiz
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class SolutionButton : ButtonListener("solution") {
    override fun process(e: ButtonInteractionEvent, name: String) {
        val split = Command.TRIPLE_HASHTAG.split(name)
        val tcid = split[0].toLong()
        val uid = split[1].toLong()
        val round = split[2].toInt()
        if (e.user.idLong == uid) {
            e.reply("Du hast diese Abstimmung erstellt, daher kannst du selbst nicht abstimmen!").setEphemeral(true)
                .queue()
            return
        }
        val quiz = DexQuiz.getByTC(tcid)
        if (quiz == null) {
            e.reply("Dieses DexQuiz existiert nicht mehr!").setEphemeral(true).queue()
            return
        }
        if (quiz.round != round.toLong()) {
            e.reply("Diese Abstimmung war für Runde %d!".formatted(round)).setEphemeral(true).queue()
            return
        }
        e.reply("\uD83D\uDC4D").setEphemeral(true).queue()
        e.channel.sendMessage("Die Lösung ist **" + quiz.currentGerName + "**! (Eintrag aus Pokemon **" + quiz.currentEdition + "**)")
            .queue()
        quiz.nextRound()
    }
}