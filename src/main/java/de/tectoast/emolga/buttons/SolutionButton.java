package de.tectoast.emolga.buttons;

import de.tectoast.emolga.utils.DexQuiz;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import static de.tectoast.emolga.commands.Command.TRIPLE_HASHTAG;

public class SolutionButton extends ButtonListener {

    public SolutionButton() {
        super("solution");
    }

    @Override
    public void process(ButtonInteractionEvent e, String name) {
        String[] split = TRIPLE_HASHTAG.split(name);
        long tcid = Long.parseLong(split[0]);
        long uid = Long.parseLong(split[1]);
        int round = Integer.parseInt(split[2]);
        if (e.getUser().getIdLong() == uid) {
            e.reply("Du hast diese Abstimmung erstellt, daher kannst du selbst nicht abstimmen!").setEphemeral(true).queue();
            return;
        }
        DexQuiz quiz = DexQuiz.getByTC(tcid);
        if (quiz == null) {
            e.reply("Dieses DexQuiz existiert nicht mehr!").setEphemeral(true).queue();
            return;
        }
        if (quiz.getRound() != round) {
            e.reply("Diese Abstimmung war für Runde %d!".formatted(round)).setEphemeral(true).queue();
            return;
        }
        e.reply("\uD83D\uDC4D").setEphemeral(true).queue();
        e.getChannel().sendMessage("Die Lösung ist **" + quiz.getCurrentGerName() + "**! (Eintrag aus Pokemon **" + quiz.getCurrentEdition() + "**)").queue();
        quiz.nextRound();
    }
}
