package de.tectoast.emolga.buttons.buttonsaves;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TrainerData {
    private static final Logger logger = LoggerFactory.getLogger(TrainerData.class);
    final LinkedHashMap<String, List<TrainerMon>> mons = new LinkedHashMap<>();
    String current;
    private boolean withMoveset = false;

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
                    String alt = t.child(0).child(0).child(0).attr("alt");
                    String monname = alt.startsWith("Sugimori") ? t.child(1).text() : alt;
                    list.add(new TrainerMon(monname, t.child(2).child(0).child(0).text().substring(4), (t.children().size() == 11 ? t.child(4).text() : null), !itm.contains("Kein Item") ? itm : null, moves));
                }
                mons.put(set.replace("*", "").trim(), list);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void swapWithMoveset() {
        withMoveset = !withMoveset;
    }

    public boolean isWithMoveset() {
        return withMoveset;
    }

    public void setCurrent(String fight) {
        this.current = fight;
    }

    public boolean isCurrent(String fight) {
        return Objects.equals(current, fight);
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
        logger.info("getNormalName name = {}", name);
        String s = mons.keySet().stream().filter(name::equalsIgnoreCase).findFirst().orElse(null);
        logger.info("s = {}", s);
        return s;
    }

    public record TrainerMon(String name, String level, String ability, String item,
                             List<String> moves) {

        public String build(boolean onlyWithLevel) {
            if (onlyWithLevel) return name + " (Level " + level + ")";
            return name + (item != null && !item.trim().isEmpty() ? " @ " + item : "") + "\n"
                   + (ability != null ? "Fähigkeit: " + ability + "\n" : "")
                   + "Level: " + level + "\n"
                   + moves.stream().map(s -> "- " + s).collect(Collectors.joining("\n"));
        }
    }
}
