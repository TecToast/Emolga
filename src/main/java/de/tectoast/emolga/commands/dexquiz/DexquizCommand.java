package de.tectoast.emolga.commands.dexquiz;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.DexQuiz;
import de.tectoast.emolga.utils.sql.DBManagers;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.internal.utils.tuple.Pair;

public class DexquizCommand extends Command {
    public DexquizCommand() {
        super("dexquiz", "Erstellt ein Dexquiz mit der angegebenen Anzahl an Einträgen", CommandCategory.Dexquiz);
        setArgumentTemplate(ArgumentManagerTemplate.builder().add("count", "Anzahl", "Die Anzahl an Einträgen", ArgumentManagerTemplate.DiscordType.INTEGER, true)
                .setExample("!dexquiz 10")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        String msg = e.getMessage().getContentDisplay();
        DexQuiz quiz = DexQuiz.getByTC(tco);
        if (quiz != null) {
            tco.sendMessage("Die Lösung des alten Quizzes war " + quiz.getCurrentGerName() + "!").queue();
            DexQuiz.removeByTC(tco.getIdLong());
        } else {
            try {
                ArgumentManager args = e.getArguments();
                if (!args.has("count")) {
                    e.reply("Du musst eine Rundenanzahl angeben! (Bspw. `!dexquiz 10`)");
                }
                Pair<String, String> pair = DexQuiz.getNewMon();
                String pokemon = pair.getLeft();
                String englName = pair.getRight();
                sendDexEntry(tco.getAsMention() + " " + pokemon);
                Pair<String, String> res = DBManagers.POKEDEX.getDexEntry(pokemon);
                String entry = res.getLeft();
                new DexQuiz(tco, pokemon, englName, args.getInt("count"), res.getRight());
                //� = %C3%B6
                tco.sendMessage("Runde 1: " + trim(entry, pokemon) + "\nZu welchem Pokemon gehört dieser Dex-Eintrag?").queue();
            } catch (Exception ioException) {
                tco.sendMessage("Es ist ein Fehler aufgetreten!").queue();
                ioException.printStackTrace();
            }
        }
    }
}
