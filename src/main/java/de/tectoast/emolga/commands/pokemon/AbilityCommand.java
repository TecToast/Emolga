package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.jsolf.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class AbilityCommand extends Command {

    public AbilityCommand() {
        super("ability", "Zeigt, welche Mons eine gewisse Fähigkeit haben können", CommandCategory.Pokemon);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .addEngl("abi", "Fähigkeit", "Name der Fähigkeit, nach der geschaut werden soll", Translation.Type.ABILITY, false)
                .setExample("!ability Intimidate")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        JSONObject json = getDataJSON(getModByGuild(e));
        ArrayList<String> mons = new ArrayList<>();
        String abi = e.getArguments().getTranslation("abi").getTranslation();
        for (String s : json.keySet()) {
            JSONObject data = json.getJSONObject(s);
            if (data.getInt("num") < 0) continue;
            if (data.getJSONObject("abilities").keySet().stream().map(ability -> data.getJSONObject("abilities").getString(ability)).anyMatch(ability -> ability.equalsIgnoreCase(abi))) {
                String name;
                if (s.equals("nidoranf")) name = "Nidoran-F";
                else if (s.equals("nidoranm")) name = "Nidoran-M";
                else {
                    Translation gerName = getGerName(s);
                    if (gerName.isSuccess()) name = gerName.getTranslation();
                    else {
                        name = getGerNameNoCheck(data.getString("baseSpecies")) + "-" + data.getString("forme");
                    }
                }
                mons.add(name);
            }
        }
        mons.removeIf(Objects::isNull);
        Collections.sort(mons);
        StringBuilder s = new StringBuilder(2 << 11);
        for (String mon : mons) {
            s.append(mon).append("\n");
            if (s.length() > 1900) {
                e.reply(new EmbedBuilder().setColor(Color.CYAN).setTitle(e.getArguments().getTranslation("abi").getOtherLang() + " haben:").setDescription(s).build());
                s = new StringBuilder(2 << 11);
            }
        }
        e.reply(new EmbedBuilder().setColor(Color.CYAN).setTitle(e.getArguments().getTranslation("abi").getOtherLang() + " haben:").setDescription(s).build());
        /*
        try {
            tco.sendMessage(new EmbedBuilder().setColor(Color.CYAN).setTitle(eachWordUpperCase(msg.substring(9)) + " haben:").setDescription(String.join("\n", Jsoup.connect("https://www.pokewiki.de/" + msg.substring(9)).get().select("span[style=\"padding-left: 0.2em;\"]").stream().map(Element::text).collect(Collectors.toCollection(ArrayList::new)))).build()).queue();
        } catch (IOException e) {
            e.printStackTrace();
            tco.sendMessage("Es ist ein Fehler aufgetreten!").queue();
        }*/

    }
}
