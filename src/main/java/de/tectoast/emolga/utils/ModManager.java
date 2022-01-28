package de.tectoast.emolga.utils;

import de.tectoast.emolga.commands.Command;
import org.jsolf.JSONObject;

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
    private JSONObject typechart;

    public ModManager(String name, String datapath) {
        this.name = name;
        this.datapath = datapath;
        File dir = new File(datapath);
        new Thread(() -> this.dex = Command.loadSD(datapath + "pokedex.ts", Constants.DEXJSONSUB), "ModManager " + name + " Dex").start();
        new Thread(() -> this.learnsets = Command.loadSD(datapath + "learnsets.ts", Constants.LEARNSETJSONSUB), "ModManager " + name + " Learnsets").start();
        new Thread(() -> this.moves = Command.loadSD(datapath + "moves.ts", Constants.MOVESJSONSUB), "ModManager " + name + " Moves").start();
        new Thread(() -> this.typechart = Command.loadSD(datapath + "typechart.ts", Constants.TYPESJSONSUB), "ModManager " + name + " Typechart").start();
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

    public JSONObject getTypechart() {
        return typechart;
    }

    public void update() {
        this.dex = Command.loadSD(datapath + "pokedex.ts", Constants.DEXJSONSUB);
        this.learnsets = Command.loadSD(datapath + "learnsets.ts", Constants.LEARNSETJSONSUB);
        this.moves = Command.loadSD(datapath + "moves.ts", Constants.MOVESJSONSUB);
        this.typechart = Command.loadSD(datapath + "typechart.ts", Constants.TYPESJSONSUB);
    }
}
