package de.tectoast.emolga.commands.dexquiz;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.Constants;
import de.tectoast.emolga.utils.DexQuiz;
import net.dv8tion.jda.api.entities.TextChannel;

public class TipCommand extends Command {
    public TipCommand() {
        super("tip", "Zeigt einen Tipp für den derzeitigen Eintrag", CommandCategory.Dexquiz);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("tip", "Tipp", "Der Tipp, den du haben möchtest", ArgumentManagerTemplate.Text.of(
                        DexQuizTip.buildSubcommands(), true
                ))
                .setExample("/tip anfangsbuchstabe")
                .build());
        slash(true, 918865966136455249L, Constants.FPLID);
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        DexQuiz quiz = DexQuiz.getByTC(tco);
        if (quiz != null) {
            String tip = e.getArguments().getText("tip").toUpperCase();
            long newBudget = quiz.useTip(e.getAuthor().getIdLong(), tip);
            if (newBudget == -10) {
                e.reply("Dieser Tipp ist auf diesem Server zurzeit deaktiviert! Nutze `/configurate dexquiz` um dies zu ändern!", true);
                return;
            }
            if (newBudget < 0) {
                e.reply("Dafür hast du nicht mehr genug Budget!", true);
                return;
            }
            e.reply(DexQuizTip.valueOf(tip).getTipFunction().apply(
                    new DexQuizTip.TipData(quiz.getCurrentGerName(), quiz.getCurrentEnglName(), quiz.getCurrentEdition(),
                            Command.getDataObject(quiz.getCurrentGerName()))
            ) + "\nNeuer Kontostand: **%d**".formatted(newBudget), true);
        } else {
            e.reply("In diesem Channel wurde kein Dexquiz erstellt!", true);
        }
    }
}
