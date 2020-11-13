package de.Flori.Commands.DexQuiz;

import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import de.Flori.utils.DexQuiz;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SolutionCommand extends Command {
    public SolutionCommand() {
        super("solution", "`!solution` Zeigt die Lösung des derzeitigen Eintrags", CommandCategory.Dexquiz);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        String msg = e.getMessage().getContentDisplay();
        Member member = e.getMember();
        DexQuiz quiz = DexQuiz.getByTC(tco);
        if (quiz != null) {
            tco.sendMessage("Die Loesung ist " + quiz.gerName + "!").queue();
            quiz.round++;
            if (!quiz.points.containsKey(member)) quiz.points.put(member, 0);
            quiz.points.put(member, quiz.points.get(member) + 1);
            if (quiz.round > quiz.cr) {
                StringBuilder builder = new StringBuilder("Punkte:\n");
                for (Map.Entry<Member, Integer> en : quiz.points.entrySet()) {
                    builder.append(en.getKey().getAsMention()).append(": ").append(en.getValue()).append("\n");
                }
                tco.sendMessage(builder.toString()).queue();
                DexQuiz.list.remove(quiz);
                return;
            }
            File file = new File("./entwicklung.txt");
            try {
                List<String> list = Files.readAllLines(file.toPath());
                String pokemon = list.get(new Random().nextInt(list.size()));
                Document d = Jsoup.connect("https://www.pokewiki.de/" + pokemon).get();
                String englName = getEnglName(pokemon);
                Element table = d.select("table[class=\"round centered\"]").get(0);
                Element element = table.select("td").get(new Random().nextInt(table.select("td").size()));
                quiz.gerName = pokemon;
                sendToMe(pokemon);
                quiz.englName = englName;
                //� = %C3%B6
                tco.sendMessage(trim(element.text(), pokemon) + "\nZu welchem Pokemon gehört dieser Dex-Eintrag?").queue();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } else {
            tco.sendMessage("In diesem Channel wurde kein DexQuiz erstellt!").queue();
        }
    }
}
