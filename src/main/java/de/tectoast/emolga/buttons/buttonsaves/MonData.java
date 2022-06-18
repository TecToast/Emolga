package de.tectoast.emolga.buttons.buttonsaves;

import de.tectoast.jsolf.JSONObject;

import java.util.LinkedHashMap;
import java.util.List;

import static de.tectoast.emolga.commands.Command.toSDName;

public class MonData {

    final LinkedHashMap<String, JSONObject> data = new LinkedHashMap<>();
    final List<JSONObject> list;

    public MonData(List<JSONObject> l) {
        list = l;
        l.forEach(j -> data.put(toSDName(j.getString("name")), j));
    }

    public JSONObject getData(String id) {
        return data.get(id);
    }

    public List<JSONObject> getList() {
        return list;
    }
}
