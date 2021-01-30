package de.tectoast.commands.dexquiz;

import de.tectoast.commands.Command;
import de.tectoast.commands.CommandCategory;
import de.tectoast.utils.DexQuiz;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

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
                //noinspection SuspiciousMethodCalls
                for (Member mem : quiz.points.keySet().stream().sorted(Comparator.comparing(quiz.points::get).reversed()).collect(Collectors.toList())) {
                    builder.append(mem.getAsMention()).append(": ").append(quiz.points.get(mem)).append("\n");
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
                sendToMe(tco.getAsMention() + pokemon);
                quiz.englName = englName;
                //� = %C3%B6
                tco.sendMessage(trim(element.text(), pokemon) + "\nZu welchem pokemon gehört dieser Dex-Eintrag?").queue();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } else {
            tco.sendMessage("In diesem Channel wurde kein dexquiz erstellt!").queue();
        }
    }
}
