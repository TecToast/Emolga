package de.tectoast.emolga.commands.dexquiz;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.utils.DexQuiz;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.Random;

public class DexquizCommand extends Command {
    public DexquizCommand() {
        super("dexquiz", "`!dexquiz <Anzahl>` Erstellt ein Dexquiz mit der angegebenen Anzahl an Einträgen", CommandCategory.Dexquiz);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        String msg = e.getMessage().getContentDisplay();
        DexQuiz quiz = DexQuiz.getByTC(tco);
        if (quiz != null) {
            tco.sendMessage("Die Lösung des alten Quizzes war " + quiz.gerName + "!").queue();
            DexQuiz.list.remove(quiz);
        } else {
            try {
                Pair<String, String> pair = DexQuiz.getNewMon();
                String pokemon = pair.getLeft();
                String englName = pair.getRight();
                sendToMe(tco.getAsMention() + pokemon);
                Document d = Jsoup.connect("https://www.pokewiki.de/" + pokemon).get();
                Element table = d.select("table[class=\"round centered\"]").get(0);
                Element element = table.select("td").get(new Random().nextInt(table.select("td").size()));
                new DexQuiz(tco, pokemon, englName, Integer.parseInt(msg.substring(9)));
                //� = %C3%B6
                tco.sendMessage(trim(element.text(), pokemon) + "\nZu welchem Pokemon gehört dieser Dex-Eintrag?").queue();
            } catch (Exception ioException) {
                tco.sendMessage("Es ist ein Fehler aufgetreten!").queue();
                ioException.printStackTrace();
            }
        }
    }
}
