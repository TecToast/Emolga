package de.tectoast.emolga.selectmenus.selectmenusaves;

import de.tectoast.emolga.commands.pokemon.SmogonCommand;
import de.tectoast.jsolf.JSONArray;
import de.tectoast.jsolf.JSONObject;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SmogonSet {
    final JSONArray arr;
    JSONObject format;
    JSONObject set;
    JSONObject ev;
    JSONObject iv;
    JSONArray moveslots;

    public SmogonSet(JSONArray arr) {
        this.arr = arr;
        format = arr.getJSONObject(0);
        set = format.getJSONArray("movesets").getJSONObject(0);
        ev = set.getJSONArray("evconfigs").getJSONObject(0);
        iv = set.getJSONArray("ivconfigs").length() > 0 ? set.getJSONArray("ivconfigs").getJSONObject(0) : new JSONObject();
        moveslots = set.getJSONArray("moveslots");
    }

    public void changeFormat(String format) {
        this.format = this.arr.toJSONList().stream().filter(o -> o.getString("format").equals(format)).findFirst().orElse(null);
        this.set = this.format.getJSONArray("movesets").getJSONObject(0);
        this.ev = this.set.getJSONArray("evconfigs").getJSONObject(0);
        this.iv = this.set.getJSONArray("ivconfigs").length() > 0 ? this.set.getJSONArray("ivconfigs").getJSONObject(0) : new JSONObject();
        this.moveslots = this.set.getJSONArray("moveslots");
    }

    public void changeSet(String set) {
        this.set = this.format.getJSONList("movesets").stream().filter(o -> o.getString("name").equals(set)).findFirst().orElse(null);
        this.ev = this.set.getJSONArray("evconfigs").getJSONObject(0);
        this.iv = this.set.getJSONArray("ivconfigs").length() > 0 ? this.set.getJSONArray("ivconfigs").getJSONObject(0) : new JSONObject();
        this.moveslots = this.set.getJSONArray("moveslots");
    }

    public String buildMessage() {
        return """
                %s @ %s
                Ability: %s
                EVs: %s%s
                %s Nature
                - %s
                - %s
                - %s
                - %s""".formatted(
                set.getString("pokemon"), String.join(" / ", set.getStringList("items")),
                String.join(" / ", set.getStringList("abilities")),
                ev.keySet().stream().filter(s -> ev.getInt(s) > 0).map(s -> ev.getInt(s) + " " + SmogonCommand.statnames.get(s)).collect(Collectors.joining(" / ")),
                iv.length() > 0 ? "\nIVs: " + ev.keySet().stream().filter(s -> ev.getInt(s) > 0).map(s -> ev.getInt(s) + " " + SmogonCommand.statnames.get(s)).collect(Collectors.joining(" / ")) : "",
                String.join(" / ", set.getStringList("natures")),
                moveslots.getJSONArray(0).toJSONList().stream().map(o -> o.getString("move") + (!o.isNull("type") ? " " + o.getString("type") : "")).collect(Collectors.joining(" / ")),
                moveslots.getJSONArray(1).toJSONList().stream().map(o -> o.getString("move") + (!o.isNull("type") ? " " + o.getString("type") : "")).collect(Collectors.joining(" / ")),
                moveslots.getJSONArray(2).toJSONList().stream().map(o -> o.getString("move") + (!o.isNull("type") ? " " + o.getString("type") : "")).collect(Collectors.joining(" / ")),
                moveslots.getJSONArray(3).toJSONList().stream().map(o -> o.getString("move") + (!o.isNull("type") ? " " + o.getString("type") : "")).collect(Collectors.joining(" / "))
        );
    }

    public List<ActionRow> buildActionRows() {
        return Arrays.asList(
                ActionRow.of(SelectMenu.create("smogonformat").addOptions(
                        arr.toJSONList().stream().map(o -> o.getString("format")).map(s -> SelectOption.of("Format: " + s, s).withDefault(format.getString("format").equals(s))).collect(Collectors.toList())
                ).build()),
                ActionRow.of(SelectMenu.create("smogonset").addOptions(
                        format.getJSONList("movesets").stream().map(o -> o.getString("name")).map(s -> SelectOption.of("Set: " + s, s).withDefault(set.getString("name").equals(s))).collect(Collectors.toList())
                ).build()));
    }
}
