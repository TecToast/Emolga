package de.tectoast.commands.dexquiz;

import de.tectoast.commands.Command;
import de.tectoast.commands.CommandCategory;
import de.tectoast.utils.DexQuiz;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
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
            File file = new File("./entwicklung.txt");
            try {
                List<String> list = Files.readAllLines(file.toPath());
                String pokemon = list.get(new Random().nextInt(list.size()));
                sendToMe(tco.getAsMention() + pokemon);
                Document d = Jsoup.connect("https://www.pokewiki.de/" + pokemon).get();
                String englName = getEnglName(pokemon);
                Element table = d.select("table[class=\"round centered\"]").get(0);
                Element element = table.select("td").get(new Random().nextInt(table.select("td").size()));
                new DexQuiz(tco, pokemon, englName, Integer.parseInt(msg.substring(9)));
                //� = %C3%B6
                tco.sendMessage(trim(element.text(), pokemon) + "\nZu welchem pokemon gehört dieser Dex-Eintrag?").queue();
            } catch (Exception ioException) {
                tco.sendMessage("Es ist ein Fehler aufgetreten!").queue();
                ioException.printStackTrace();
            }
        }
    }
}
