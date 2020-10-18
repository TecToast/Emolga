package de.Flori.Commands.DexQuiz;

import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import de.Flori.utils.DexQuiz;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class TipCommand extends Command {
    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        String msg = e.getMessage().getContentDisplay();
        Member member = e.getMember();
        DexQuiz quiz = DexQuiz.getByTC(tco);
        if (quiz != null) {
            tco.sendMessage("Anfangsbuchstabe der Lösung: " + quiz.gerName.charAt(0) + "\n" +
                    "Anfangsbuchstabe auf Englisch: " + quiz.englName.charAt(0)).queue();
        } else {
            tco.sendMessage("In diesem Channel wurde kein DexQuiz erstellt!").queue();
        }
    }

    public TipCommand() {
        super("tip", "`!tip` Zeigt einen Tipp für den derzeitigen Eintrag", CommandCategory.Dexquiz);
    }
}
