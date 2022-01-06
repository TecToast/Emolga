package de.tectoast.emolga.utils;

import de.tectoast.emolga.utils.sql.DBManagers;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.internal.utils.tuple.ImmutablePair;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
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
    public String edition;
    public int round = 1;
    public boolean block = false;

    public DexQuiz(TextChannel tc, String gerName, String englName, int rounds, String edition) {
        this.tc = tc;
        this.gerName = gerName;
        this.englName = englName;
        this.cr = rounds;
        this.edition = edition;
        if (cr <= 0) {
            tc.sendMessage("Du musst eine Mindestanzahl von einer Runde angeben!").queue();
            return;
        }
        list.add(this);
    }

    public static Pair<String, String> getNewMon() {
        try {
            if (cachedMons == null) {
                File file = new File("./entwicklung.txt");
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

    public boolean check(String t) {
        return t.equalsIgnoreCase(gerName) || t.equalsIgnoreCase(englName);
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
        Pair<String, String> mon = getNewMon();
        String pokemon = mon.getLeft();
        String englName = mon.getRight();
            /*Document d = Jsoup.connect("https://www.pokewiki.de/" + pokemon).get();
            Element table = d.select("table[class=\"round centered\"]").get(0);
            Element element = table.select("td").get(new Random().nextInt(table.select("td").size()));*/
        Pair<String, String> res = DBManagers.POKEDEX.getDexEntry(pokemon);
        String entry = res.getLeft();
        edition = res.getRight();
        gerName = pokemon;
        sendDexEntry(this.tc.getAsMention() + pokemon);
        this.englName = englName;
        //ü = %C3%B6
        this.block = false;
        this.tc.sendMessage("Runde " + round + ": " + trim(entry, pokemon) + "\nZu welchem Pokemon gehört dieser Dex-Eintrag?").queueAfter(3, TimeUnit.SECONDS);
    }
}
