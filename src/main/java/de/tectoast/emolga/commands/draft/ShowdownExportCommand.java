package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.jsolf.JSONArray;
import de.tectoast.jsolf.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import static de.tectoast.emolga.bot.EmolgaMain.emolgajda;

public class ShowdownExportCommand extends Command {
    public ShowdownExportCommand() {
        super("showdownexport", "Macht Showdown Export lol", CommandCategory.Pokemon);
        setArgumentTemplate(ArgumentManagerTemplate.builder().add("draft", "Draft-Name", "Der Name der Liga, für die der Export gemacht werden soll", ArgumentManagerTemplate.draft())
                .setExample("!showdownexport Emolga-Conference")
                .build());
        setCustomPermissions(PermissionPreset.CULT);
    }

    @Override
    public void process(GuildCommandEvent e) {
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject(e.getArguments().getText("draft"));
        JSONObject picksObj = league.getJSONObject("picks");
        StringBuilder b = new StringBuilder();
        ArrayList<String> ids = new ArrayList<>(picksObj.keySet());
        HashMap<String, String> names = new HashMap<>();
        emolgajda.getGuildById(league.getString("guild")).retrieveMembersByIds(ids.toArray(new String[0])).get().forEach(mem -> names.put(mem.getId(), mem.getEffectiveName()));
        for (String id : ids) {
            JSONArray picksArr = picksObj.getJSONArray(id);
            JSONArray oneUser = new JSONArray();
            b.append("=== [gen8nationaldexag-box] ").append(e.getArg(0)).append("/").append(names.get(id)).append(" ===\n\n");
            picksArr.toList().stream().map(o -> (String) ((HashMap<String, Object>) o).get("name"))
                    .sorted(Comparator.comparing(str -> getDataJSON().getJSONObject(getDataName((String) str)).getJSONObject("baseStats").getInt("spe")).reversed())
                    .map(str -> {
                                if (sdex.containsKey(str)) {
                                    return getEnglName(getFirst(str)) + getFirstAfterUppercase(sdex.get(str)) + " \nAbility: " + getDataJSON().getJSONObject(getDataName(str)).getJSONObject("abilities").getString("0");
                                }
                                if (str.startsWith("A-")) {
                                    return getEnglName(str.substring(2)) + "-Alola \nAbility: " + getDataJSON().getJSONObject(getDataName(str)).getJSONObject("abilities").getString("0");
                                }
                                if (str.startsWith("G-")) {
                                    return getEnglName(str.substring(2)) + "-Galar \nAbility: " + getDataJSON().getJSONObject(getDataName(str)).getJSONObject("abilities").getString("0");
                                }
                                if (str.startsWith("M-")) {
                                    return getEnglName(str.substring(2)) + "-Mega \nAbility: " + getDataJSON().getJSONObject(getDataName(str)).getJSONObject("abilities").getString("0");
                                }
                                return getEnglName(str) + " \nAbility: " + getDataJSON().getJSONObject(getDataName(str)).getJSONObject("abilities").getString("0");
                            }
                    ).forEach(str -> b.append(str).append("\n\n"));
            b.append("\n");
            if (b.length() > 1500) {
                e.reply(b.toString());
                b.setLength(0);
            }
        }
        e.reply(b.toString());
    }
}
