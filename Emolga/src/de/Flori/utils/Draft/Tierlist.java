package de.Flori.utils.Draft;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class Tierlist {
    public static final ArrayList<Tierlist> list = new ArrayList<>();
    public final HashMap<String, ArrayList<String>> tierlist = new HashMap<>();
    public final HashMap<String, Integer> prices = new HashMap<>();
    public final String guild;
    public final ArrayList<String> tiercolumns = new ArrayList<>();
    public final ArrayList<String> order = new ArrayList<>();
    public boolean isPointBased;

    public Tierlist(String guild) {
        this.guild = guild;
        File dir = new File("./Tierlists/" + guild + "/");
        for (File file : dir.listFiles()) {
            try {
                List<String> lines = Files.readAllLines(file.toPath());
                String name = file.getName().substring(0, file.getName().length() - 4);
                if (name.equals("data")) {
                    isPointBased = lines.get(0).equalsIgnoreCase("points");
                    order.addAll(Arrays.asList(lines.get(1).split(",")));
                } else if (name.equals("tiercolumns")) {
                    tiercolumns.addAll(lines);
                } else {
                    prices.put(name, Integer.parseInt(lines.remove(0)));
                    tierlist.put(name, new ArrayList<>(lines));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        list.add(this);
    }

    public static void setup() {
        list.clear();
        File dir = new File("./Tierlists/");
        for (File file : dir.listFiles()) {
            new Tierlist(file.getName());
        }
    }

    public static Tierlist getByGuild(String guild) {
        for (Tierlist tierlist : list) {
            if (tierlist.guild.equals(guild)) return tierlist;
        }
        return null;
    }

    public int getPointsNeeded(String s) {
        for (Map.Entry<String, ArrayList<String>> en : tierlist.entrySet()) {
            if (en.getValue().contains(s)) return prices.get(en.getKey());
        }
        return -1;
    }

    public String getTierOf(String s) {
        for (Map.Entry<String, ArrayList<String>> en : tierlist.entrySet()) {
            if (en.getValue().stream().anyMatch(s::equalsIgnoreCase)) return en.getKey();
        }
        return "";
    }

    public String getNameOf(String s) {
        for (Map.Entry<String, ArrayList<String>> en : tierlist.entrySet()) {
            String str = en.getValue().stream().filter(s::equalsIgnoreCase).collect(Collectors.joining(""));
            if (!str.equals("")) return str;
        }
        return "";
    }
}
