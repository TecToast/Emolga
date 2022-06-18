package de.tectoast.emolga.commands.dexquiz;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.Constants;
import de.tectoast.emolga.utils.DexQuiz;
import net.dv8tion.jda.api.entities.TextChannel;

public class DexquizCommand extends Command {
    public DexquizCommand() {
        super("dexquiz", "Erstellt ein Dexquiz mit der angegebenen Anzahl an Einträgen", CommandCategory.Dexquiz);
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
        slash(true, 918865966136455249L, Constants.FPLID);
    }

    @Override
    public void process(GuildCommandEvent e) throws Exception {

    }

    public static class Start extends Command {

        public Start() {
            super("start", "Startet ein Dexquiz");
            setArgumentTemplate(ArgumentManagerTemplate.builder()
                    .add("count", "Rundenanzahl", "Die Anzahl an Runden, die du spielen möchtest", ArgumentManagerTemplate.DiscordType.INTEGER)
                    .build());
        }

        @Override
        public void process(GuildCommandEvent e) throws Exception {
            TextChannel tco = e.getChannel();
            DexQuiz quiz = DexQuiz.getByTC(tco);
            if (quiz != null) {
                e.reply("In diesem Channel läuft bereits ein Dexquiz! Wenn du dieses beenden möchtest, verwende `/dexquiz end`.");
                return;
            }
            try {
                new DexQuiz(tco, e.getArguments().getLong("count"));
                if (e.isSlash()) e.getSlashCommandEvent().reply("\uD83D\uDC4D").setEphemeral(true).queue();
            } catch (Exception ioException) {
                tco.sendMessage("Es ist ein Fehler aufgetreten!").queue();
                ioException.printStackTrace();
            }

        }
    }

    public static class End extends Command {

        public End() {
            super("end", "Beendet das Dexquiz in diesem Channel");
            setArgumentTemplate(ArgumentManagerTemplate.noArgs());
        }

        @Override
        public void process(GuildCommandEvent e) throws Exception {
            TextChannel tco = e.getChannel();
            DexQuiz quiz = DexQuiz.getByTC(tco);
            if (quiz != null) {
                e.reply("Die Lösung des alten Quizzes war " + quiz.getCurrentGerName() + "!");
                quiz.end();
            }
        }
    }
}
