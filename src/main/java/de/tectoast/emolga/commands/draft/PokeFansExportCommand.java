package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.draft.Draft;
import org.jsolf.JSONArray;
import org.jsolf.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import static de.tectoast.emolga.bot.EmolgaMain.emolgajda;

public class PokeFansExportCommand extends Command {
    public PokeFansExportCommand() {
        super("pokefansexport", "Macht Pokefans Export lol", CommandCategory.Draft);
        setArgumentTemplate(ArgumentManagerTemplate.builder().add("draft", "Draft-Name", "Der Name der Liga, für die der Export gemacht werden soll", ArgumentManagerTemplate.draft())
                .setExample("!pokefansexport Emolga-Conference")
                .build());
        setCustomPermissions(PermissionPreset.CULT);
    }

    @Override
    public void process(GuildCommandEvent e) {
        JSONObject league = Draft.getLeagueStatic(e.getArguments().getText("draft"));
        JSONObject picksObj = league.getJSONObject("picks");
        JSONArray tosend = new JSONArray();
        ArrayList<String> ids = new ArrayList<>(picksObj.keySet());
        HashMap<String, String> names = new HashMap<>();
        emolgajda.getGuildById(league.getString("guild")).retrieveMembersByIds(ids.toArray(new String[0])).get().forEach(mem -> names.put(mem.getId(), mem.getEffectiveName()));
        for (String id : ids) {
            JSONArray picksArr = picksObj.getJSONArray(id);
            JSONArray oneUser = new JSONArray();
            oneUser.put(names.get(id).replaceAll("[^A-Za-z\\s]", ""));
            oneUser.put(e.getArg(0));
            JSONArray mons = new JSONArray();
            picksArr.toList().stream().map(o -> (String) ((HashMap<String, Object>) o).get("name"))
                    .sorted(Comparator.comparing(str -> getDataJSON().getJSONObject(getDataName((String) str)).getJSONObject("baseStats").getInt("spe")).reversed())
                    .map(str -> str
                            .replace("Boreos-T", "Boreos Tiergeistform")
                            .replace("Voltolos-T", "Voltolos Tiergeistform")
                            .replace("Demeteros-T", "Demeteros Tiergeistform")
                            .replace("Boreos-I", "Boreos Inkarnationsform")
                            .replace("Voltolos-I", "Voltolos Inkarnationsform")
                            .replace("Demeteros-I", "Demeteros Inkarnationsform")
                            .replace("Wolwerock-Tag", "Wolwerock Tagform")
                            .replace("Wolwerock-Nacht", "Wolwerock Nachtform")
                            .replace("Wolwerock-Zw", "Wolwerock Zwielichtform")
                            .replace("Shaymin", "Shaymin Landform")
                            .replace("Durengard", "Durengard Schildform")
                            .replace("Pumpdjinn", "Pumpdjinn XL")
                            .replace("M-", "Mega-")
                            .replace("A-", "Alola-")
                            .replace("G-", "Galar-")
                    ).forEach(mons::put);
            oneUser.put(mons);
            tosend.put(oneUser);
            if(tosend.toString().length() > 1500) {
                e.reply(tosend.toString());
                tosend.clear();
            }
        }
        e.reply(tosend.toString());
    }
}
