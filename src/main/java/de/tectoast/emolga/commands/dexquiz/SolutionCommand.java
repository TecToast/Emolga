package de.tectoast.emolga.commands.dexquiz;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.Constants;
import de.tectoast.emolga.utils.DexQuiz;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;

public class SolutionCommand extends Command {
    public SolutionCommand() {
        super("solution", "Zeigt die Lösung des derzeitigen Eintrags", CommandCategory.Dexquiz);
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
        slash(true, 918865966136455249L, Constants.FPLID);
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        DexQuiz quiz = DexQuiz.getByTC(tco);
        if (quiz != null) {
            e.getSlashCommandEvent().replyEmbeds(new EmbedBuilder()
                            .setTitle("%s hat vorgeschlagen, die Lösung aufzudecken!".formatted(e.getMember().getEffectiveName()))
                            .setDescription("Wenn eine weitere Person auf den Button drückt, wird die Lösung aufgedeckt!")
                            .setColor(Color.CYAN)
                            .build())
                    .addActionRow(Button.primary("solution;%d###%d###%d".formatted(tco.getIdLong(), e.getAuthor().getIdLong(), quiz.getRound()), "Lösung aufdecken"))
                    .queue();
        } else {
            tco.sendMessage("In diesem Channel wurde kein Dexquiz erstellt!").queue();
        }
    }
}
