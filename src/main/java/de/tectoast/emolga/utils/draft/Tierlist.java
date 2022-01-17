package de.tectoast.emolga.utils.draft;

import de.tectoast.emolga.commands.Command;
import net.dv8tion.jda.internal.utils.tuple.ImmutablePair;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jsolf.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static de.tectoast.emolga.commands.Command.load;

public class Tierlist {
    /**
     * All tierlists
     */
    public static final ArrayList<Tierlist> list = new ArrayList<>();
    private static final Logger logger = LoggerFactory.getLogger(Tierlist.class);
    /**
     * HashMap containing<br>Keys: Tiers<br>Values: Lists with the mons
     */
    public final Map<String, List<String>> tierlist = new LinkedHashMap<>();
    /**
     * The price for each tier
     */
    public final Map<String, Integer> prices = new HashMap<>();
    /**
     * The guild of this tierlist
     */
    public final String guild;
    /**
     * List with all pokemon in the sheets tierlists, columns are separated by an "NEXT"
     */
    public final LinkedList<String> tiercolumns = new LinkedList<>();
    /**
     * Order of the tiers, from highest to lowest
     */
    public final List<String> order = new ArrayList<>();
    /**
     * if this tierlist is pointbased
     */
    public boolean isPointBased;
    /**
     * the possible points for a player
     */
    public int points;
    /**
     * the amount of rounds in the draft
     */
    public final int rounds;

    public Tierlist(String guild) {
        this.guild = guild.substring(0, guild.length() - 5);
        JSONObject o = load("./Tierlists/" + guild);
        rounds = o.optInt("rounds", -1);
        String mode = o.getString("mode");
        if (rounds == -1 && !mode.equals("nothing"))
            throw new IllegalArgumentException("BRUDER OLF IST DAS DEIN SCHEIß ERNST");
        switch (mode) {
            case "points" -> {
                points = o.getInt("points");
                isPointBased = true;
            }
            case "tiers", "nothing" -> isPointBased = false;
            default -> throw new IllegalArgumentException("Invalid mode! Has to be one of 'points', 'tiers' or 'nothing'!");
        }
        JSONObject tiers = o.getJSONObject("tiers");
        for (String s : tiers.keySet()) {
            order.add(s);
            prices.put(s, tiers.getInt(s));
        }
        List<List<String>> mons = o.getJSONArray("mons").toListList(String.class);
        int x = 0;
        int currtier = 0;
        List<String> currtierlist = new LinkedList<>();
        List<Integer> nexttiers = o.getIntList("nexttiers");
        for (List<String> mon : mons) {
            if (nexttiers.contains(x)) {
                String key = order.get(currtier++);
                tierlist.put(key, new ArrayList<>(currtierlist));
                currtierlist.clear();
            }
            currtierlist.addAll(mon);
            tiercolumns.addAll(mon);
            tiercolumns.add("NEXT");
            x++;
        }
        tierlist.put(order.get(currtier), new ArrayList<>(currtierlist));
        tiercolumns.removeLast();
        /*File dir = new File("./Tierlists/" + guild + ".json");
        for (File file : dir.listFiles()) {
            try {
                List<String> lines = Files.readAllLines(file.toPath());
                String name = file.getName().split("\\.")[0];
                if (name.equals("data")) {
                    JSONObject o = null;
                    try {
                        o = new JSONObject(String.join("\n", lines));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    rounds = o.optInt("rounds", -1);
                    String mode = o.getString("mode");
                    if (rounds == -1 && !mode.equals("nothing"))
                        throw new IllegalArgumentException("BRUDER OLF IST DAS DEIN SCHEIß ERNST");
                    switch (mode) {
                        case "points" -> {
                            points = o.getInt("points");
                            isPointBased = true;
                        }
                        case "tiers", "nothing" -> isPointBased = false;
                        default -> throw new IllegalArgumentException("Invalid mode! Has to be one of 'points', 'tiers' or 'nothing'!");
                    }
                    order.addAll(o.getJSONArray("tierorder").toStringList());
                    /*String[] split = lines.get(0).split(";");
                    if(split[0].equalsIgnoreCase("points")) {
                        isPointBased = true;
                        if(split.length != 2) {
                            throw new IllegalStateException("Tierlist " + guild + " has no points!");
                        }
                        points = Integer.parseInt(split[1]);
                    } else {
                        isPointBased = false;
                    }
                    order.addAll(Arrays.asList(lines.get(1).split(",")));**
                } else if (name.equals("tiercolumns")) {
                    tiercolumns.addAll(lines.stream().map(String::trim).collect(Collectors.toCollection(ArrayList::new)));
                } else {
                    try {
                        prices.put(name, Integer.parseInt(lines.remove(0)));
                    } catch (NumberFormatException e) {
                        logger.info("guild = " + guild);
                        logger.info("name = " + name);
                    }
                    tierlist.put(name, lines.stream().map(String::trim).collect(Collectors.toCollection(ArrayList::new)));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }*/
        list.add(this);
    }

    public static void setup() {
        list.clear();
        File dir = new File("./Tierlists/");
        for (File file : dir.listFiles()) {
            if (file.isFile())
                new Tierlist(file.getName());
        }
    }

    public static Tierlist getByGuild(long guild) {
        return getByGuild(String.valueOf(guild));
    }

    public static Tierlist getByGuild(String guild) {
        for (Tierlist tierlist : list) {
            if (tierlist.guild.equals(guild)) return tierlist;
        }
        logger.error(guild + " RETURNED NULL");
        return null;
    }

    public int getPointsNeeded(String s) {
        for (Map.Entry<String, List<String>> en : tierlist.entrySet()) {
            if (en.getValue().stream().anyMatch(s::equalsIgnoreCase))
                return prices.get(en.getKey());
        }
        return -1;
    }

    public String getTierOf(String s) {
        for (Map.Entry<String, List<String>> en : tierlist.entrySet()) {
            if (en.getValue().stream().anyMatch(str -> Command.toSDName(str).equals(Command.toSDName(s))))
                return en.getKey();
        }
        return "";
    }

    public String getNameOf(String s) {
        for (Map.Entry<String, List<String>> en : tierlist.entrySet()) {
            String str = en.getValue().stream().filter(s::equalsIgnoreCase).collect(Collectors.joining(""));
            if (!str.equals("")) return str;
        }
        return "";
    }

    public Pair<Integer, Integer> getLocation(String mon, int defX, int defY) {
        int x = defX;
        int y = defY;
        for (String s : tiercolumns) {
            if (s.equalsIgnoreCase(mon)) break;
            //logger.info(s + " " + y);
            if (s.equals("NEXT")) {
                x++;
                y = 3;
            } else y++;
        }
        return new ImmutablePair<>(x, y);
    }
}