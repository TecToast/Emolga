package de.tectoast.emolga.buttons.buttonsaves;

import org.jsolf.JSONObject;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import static de.tectoast.emolga.commands.Command.toSDName;

public class MonData {

    final LinkedHashMap<String, JSONObject> data = new LinkedHashMap<>();
    final List<JSONObject> list;
    final boolean shiny;
    final List<String> sentfile = new LinkedList<>();

    public MonData(List<JSONObject> l, boolean shiny) {
        list = l;
        l.forEach(j -> data.put(toSDName(j.getString("name")), j));
        this.shiny = shiny;
    }

    public JSONObject getData(String id) {
        return data.get(id);
    }

    public boolean isShiny() {
        return shiny;
    }

    public List<JSONObject> getList() {
        return list;
    }
}
