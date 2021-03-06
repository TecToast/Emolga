package de.tectoast.emolga.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TrainerData {
    LinkedHashMap<String, List<TrainerMon>> mons = new LinkedHashMap<>();
    private boolean withMoveset = false;

    public void swapWithMoveset() {
        withMoveset = !withMoveset;
    }

    public boolean isWithMoveset() {
        return withMoveset;
    }

    public TrainerData(String trainerName) {
        Document d;
        try {
            d = Jsoup.connect("https://www.pokewiki.de/" + trainerName).get();
            Elements elelist = d.select("table[class=\"lightBg1 round darkBorder1\"]");
            for (Element elele : elelist) {
                Element ele = elele.child(0).child(1);
                String set = elele.child(0).child(0).text();
                List<TrainerMon> list = new LinkedList<>();
                for (Element child : ele.children()) {
                    Element t = child.child(0).child(0);
                    String itm = t.child(t.children().size() - 6).text().trim();
                    List<String> moves = new LinkedList<>();
                    for (int i = 7; i <= 10; i++) {
                        String text = t.child(i - (11 - t.children().size())).child(0).text();
                        if (text.trim().equals("—")) continue;
                        moves.add(text.trim());
                    }
                    list.add(new TrainerMon(t.child(0).child(0).child(0).attr("alt"), t.child(2).child(0).child(0).text().substring(4), (t.children().size() == 11 ? t.child(4).text() : null), !itm.contains("Kein Item") ? itm : null, moves));
                }
                mons.put(set.replace("*", "").trim(), list);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public HashMap<String, List<TrainerMon>> getMons() {
        return mons;
    }

    public Collection<String> getMonsList() {
        return mons.keySet();
    }

    public String getMonsFrom(String set, boolean withMoveset) {
        return mons.get(getNormalName(set)).stream().map(mon -> mon.build(!withMoveset)).collect(Collectors.joining("\n\n"));
    }

    public String getNormalName(String name) {
        System.out.println("getNormalName name = " + name);
        String s = mons.keySet().stream().filter(name::equalsIgnoreCase).findFirst().orElse(null);
        System.out.println("s = " + s);
        return s;
    }

    public static class TrainerMon {
        String name;
        String level;
        String item;
        String ability;
        List<String> moves;

        public TrainerMon(String name, String level, String ability, String item, List<String> moves) {
            this.name = name;
            this.level = level;
            this.ability = ability;
            this.item = item;
            this.moves = moves;
        }

        public String build(boolean onlyWithLevel) {
            if (onlyWithLevel) return name + " (Level " + level + ")";
            return name + (item != null && !item.trim().equals("") ? " @ " + item : "") + "\n"
                    + (ability != null ? "Fähigkeit: " + ability + "\n" : "")
                    + "Level: " + level + "\n"
                    + moves.stream().map(s -> "- " + s).collect(Collectors.joining("\n"));
        }
    }
}
