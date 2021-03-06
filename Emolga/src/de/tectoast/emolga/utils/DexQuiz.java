package de.tectoast.emolga.utils;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.internal.utils.tuple.ImmutablePair;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import static de.tectoast.emolga.commands.Command.*;

public class DexQuiz {
    public static final ArrayList<DexQuiz> list = new ArrayList<>();
    private static List<String> cachedMons;
    public final int cr;
    public final HashMap<Member, Integer> points = new HashMap<>();
    final TextChannel tc;
    public String gerName;
    public String englName;
    public int round = 1;
    public boolean block = false;

    public DexQuiz(TextChannel tc, String gerName, String englName, int rounds) {
        this.tc = tc;
        this.gerName = gerName;
        this.englName = englName;
        this.cr = rounds;
        if (cr <= 0) {
            tc.sendMessage("Du musst eine Mindestanzahl von einer Runde angeben!").queue();
            return;
        }
        list.add(this);
    }

    public static Pair<String, String> getNewMon() {
        File file = new File("./entwicklung.txt");
        try {
            if (cachedMons == null) {
                cachedMons = Files.readAllLines(file.toPath());
            }
            String pokemon = cachedMons.get(new Random().nextInt(cachedMons.size()));
            String englName = getEnglName(pokemon);
            return new ImmutablePair<>(pokemon, englName);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        return null;
    }

    public static DexQuiz getByTC(TextChannel tc) {
        return list.stream().filter(q -> q.tc.getId().equals(tc.getId())).findFirst().orElse(null);
    }

    public void end() {
        list.remove(this);
        if (points.size() == 0) {
            tc.sendMessage("Nur den Solution Command zu benutzen ist nicht der Sinn der Sache! xD").queue();
            return;
        }
        StringBuilder builder = new StringBuilder("Punkte:\n");
        //noinspection SuspiciousMethodCalls
        for (Member mem : points.keySet().stream().sorted(Comparator.comparing(points::get).reversed()).collect(Collectors.toList())) {
            builder.append(mem.getAsMention()).append(": ").append(points.get(mem)).append("\n");
        }
        tc.sendMessage(builder.toString()).queue();
    }

    public void newMon() {
        try {
            Pair<String, String> mon = getNewMon();
            String pokemon = mon.getLeft();
            String englName = mon.getRight();
            Document d = Jsoup.connect("https://www.pokewiki.de/" + pokemon).get();
            Element table = d.select("table[class=\"round centered\"]").get(0);
            Element element = table.select("td").get(new Random().nextInt(table.select("td").size()));
            gerName = pokemon;
            sendToMe(this.tc.getAsMention() + pokemon);
            this.englName = englName;
            //ü = %C3%B6
            this.block = false;
            Thread.sleep(3000);
            this.tc.sendMessage(trim(element.text(), pokemon) + "\nZu welchem Pokemon gehört dieser Dex-Eintrag?").queue();
        } catch (IOException | InterruptedException ioException) {
            ioException.printStackTrace();
        }
    }
}
