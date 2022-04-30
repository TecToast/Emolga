package de.tectoast.emolga.commands.dexquiz;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.DexQuiz;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

public class SolutionCommand extends Command {
    public SolutionCommand() {
        super("solution", "Zeigt die Lösung des derzeitigen Eintrags", CommandCategory.Dexquiz);
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        String msg = e.getMessage().getContentDisplay();
        Member member = e.getMember();
        DexQuiz quiz = DexQuiz.getByTC(tco);
        if (quiz != null) {
            tco.sendMessage("Die Lösung ist **" + quiz.getCurrentGerName() + "**! (Eintrag aus Pokemon **" + quiz.getCurrentEdition() + "**)").queue();

        } else {
            tco.sendMessage("In diesem Channel wurde kein Dexquiz erstellt!").queue();
        }
    }
}
