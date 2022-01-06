package de.tectoast.emolga.utils.draft;

import de.tectoast.emolga.commands.Command;
import org.jsolf.JSONException;
import org.jsolf.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Tierlist {
    /**
     * All tierlists
     */
    public static final ArrayList<Tierlist> list = new ArrayList<>();
    private static final Logger logger = LoggerFactory.getLogger(Tierlist.class);
    /**
     * HashMap containing<br>Keys: Tiers<br>Values: Lists with the mons
     */
    public final HashMap<String, ArrayList<String>> tierlist = new HashMap<>();
    /**
     * The price for each tier
     */
    public final HashMap<String, Integer> prices = new HashMap<>();
    /**
     * The guild of this tierlist
     */
    public final String guild;
    /**
     * List with all pokemon in the sheets tierlists, columns are separated by an "NEXT"
     */
    public final ArrayList<String> tiercolumns = new ArrayList<>();
    /**
     * Order of the tiers, from highest to lowest
     */
    public final ArrayList<String> order = new ArrayList<>();
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
    public int rounds;

    public Tierlist(String guild) {
        this.guild = guild;
        File dir = new File("./Tierlists/" + guild + "/");
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
                    order.addAll(Arrays.asList(lines.get(1).split(",")));*/
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

    public static Tierlist getByGuild(long guild) {
        return getByGuild(String.valueOf(guild));
    }

    public static Tierlist getByGuild(String guild) {
        for (Tierlist tierlist : list) {
            if (tierlist.guild.equals(guild)) return tierlist;
        }
        logger.info(guild + " RETURNED NULL");
        return null;
    }

    public int getPointsNeeded(String s) {
        for (Map.Entry<String, ArrayList<String>> en : tierlist.entrySet()) {
            if (en.getValue().stream().anyMatch(s::equalsIgnoreCase))
                return prices.get(en.getKey());
        }
        return -1;
    }

    public String getTierOf(String s) {
        for (Map.Entry<String, ArrayList<String>> en : tierlist.entrySet()) {
            if (en.getValue().stream().anyMatch(str -> Command.toSDName(str).equals(Command.toSDName(s))))
                return en.getKey();
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