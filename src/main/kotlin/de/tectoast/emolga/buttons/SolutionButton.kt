package de.tectoast.emolga.buttons

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.utils.DexQuiz
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class SolutionButton : ButtonListener("solution") {
    override fun process(e: ButtonInteractionEvent, name: String) {
        val split = Command.TRIPLE_HASHTAG.split(name)
        val round = split[2].toInt()
        if (e.user.idLong == split[1].toLong()) {
            e.reply_("Du hast diese Abstimmung erstellt, daher kannst du selbst nicht abstimmen!", ephemeral = true)
                .queue()
            return
        }
        val quiz = DexQuiz.getByTC(split[0].toLong())
        if (quiz == null) {
            e.reply_("Dieses DexQuiz existiert nicht mehr!", ephemeral = true).queue()
            return
        }
        if (quiz.round != round.toLong()) {
            e.reply_("Diese Abstimmung war für Runde $round!", ephemeral = true).queue()
            return
        }
        e.reply_("\uD83D\uDC4D", ephemeral = true).queue()
        e.channel.sendMessage("Die Lösung ist **${quiz.currentGerName}**! (Eintrag aus Pokemon **${quiz.currentEdition}**)")
            .queue()
        quiz.nextRound()
    }
}