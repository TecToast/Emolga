package de.tectoast.emolga.utils.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.utils.records.Coord;
import de.tectoast.jsolf.JSONObject;
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
     * List with all pokemon in the sheets tierlists, columns are separated by an "NEXT" SORTED BY ENGLISCH NAMES
     */
    public final LinkedList<String> tiercolumnsEngl = new LinkedList<>();
    /**
     * Order of the tiers, from highest to lowest
     */
    public final List<String> order = new ArrayList<>();
    /**
     * the amount of rounds in the draft
     */
    public final int rounds;
    /**
     * if this tierlist is pointbased
     */
    public boolean isPointBased;
    /**
     * the possible points for a player
     */
    public int points;

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
        setupTiercolumns(o.getJSONArray("mons").toListList(String.class), o.getIntList("nexttiers"), tiercolumns, true);
        if (o.has("monsengl"))
            setupTiercolumns(o.getJSONArray("monsengl").toListList(String.class), o.getIntList("nexttiers"), tiercolumnsEngl, false);
        if (o.has("trashmons")) tierlist.get(order.get(order.size() - 1)).addAll(o.getStringList("trashmons"));
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

    private void setupTiercolumns(List<List<String>> mons, List<Integer> nexttiers, List<String> tiercols, boolean normal) {
        int x = 0;
        int currtier = 0;
        List<String> currtierlist = new LinkedList<>();
        for (List<String> monss : mons) {
            List<String> mon = monss.stream().map(String::trim).toList();
            if (normal) {
                if (nexttiers.contains(x)) {
                    String key = order.get(currtier++);
                    tierlist.put(key, new ArrayList<>(currtierlist));
                    currtierlist.clear();
                }
                currtierlist.addAll(mon);
            }
            tiercols.addAll(mon);
            tiercols.add("NEXT");
            x++;
        }
        if (normal)
            tierlist.put(order.get(currtier), new ArrayList<>(currtierlist));
        tiercolumns.removeLast();
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

    public Coord getLocation(String mon) {
        return getLocation(mon, 0, 0);
    }

    public Coord getLocation(String mon, int defX, int defY) {
        return getLocation(mon, defX, defY, tiercolumns);
    }

    public Coord getLocation(String mon, int defX, int defY, List<String> tiercolumns) {
        int x = defX;
        int y = defY;
        boolean valid = false;
        for (String s : tiercolumns) {
            if (s.equalsIgnoreCase(mon)) {
                valid = true;
                break;
            }
            //logger.info(s + " " + y);
            if (s.equals("NEXT")) {
                x++;
                y = defY;
            } else y++;
        }
        return new Coord(x, y, valid);
    }
}