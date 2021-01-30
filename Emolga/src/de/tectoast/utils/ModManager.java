package de.tectoast.utils;

import de.tectoast.commands.Command;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class ModManager {
    private static final ArrayList<ModManager> modManagers = new ArrayList<>();
    private final String name;
    private final String datapath;
    private JSONObject dex;
    private JSONObject learnsets;
    private JSONObject moves;

    public ModManager(String name, String datapath) {
        this.name = name;
        this.datapath = datapath;
        File dir = new File(datapath);
        this.dex = Command.loadSD(datapath + "pokedex.ts", Constants.DEXJSONSUB);
        this.learnsets = Command.loadSD(datapath + "learnsets.ts", Constants.LEARNSETJSONSUB);
        this.moves = Command.loadSD(datapath + "moves.ts", Constants.MOVESJSONSUB);
        modManagers.add(this);
    }

    public static ModManager getByName(String name) {
        return modManagers.stream().filter(m -> m.name.equals(name)).findFirst().orElse(null);
    }

    public static ModManager getDefault() {
        return getByName("default");
    }

    public static ArrayList<String> getModList() {
        return modManagers.stream().filter(m -> !m.name.equals("default")).map(m -> m.name).collect(Collectors.toCollection(ArrayList::new));
    }

    public JSONObject getDex() {
        return dex;
    }

    public JSONObject getLearnsets() {
        return learnsets;
    }

    public JSONObject getMoves() {
        return moves;
    }

    public void update() {
        this.dex = Command.loadSD(datapath + "pokedex.ts", Constants.DEXJSONSUB);
        this.learnsets = Command.loadSD(datapath + "learnsets.ts", Constants.LEARNSETJSONSUB);
        this.moves = Command.loadSD(datapath + "moves.ts", Constants.MOVESJSONSUB);
    }
}
