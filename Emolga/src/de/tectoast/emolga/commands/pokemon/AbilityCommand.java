package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.json.JSONObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;

public class AbilityCommand extends Command {

    public AbilityCommand() {
        super("ability", "`!ability <Name>` Zeigt, welche Mons eine gewisse Fähigkeit haben können", CommandCategory.Pokemon);
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        JSONObject json = getDataJSON(getModByGuild(e));
        ArrayList<String> mons = new ArrayList<>();
        Translation t = getEnglNameWithType(msg.substring(9), getModByGuild(e));
        if (!t.isFromType(Translation.Type.ABILITY)) {
            tco.sendMessage("Das ist keine Fähigkeit!").queue();
            return;
        }
        String abi = t.getTranslation();
        for (String s : json.keySet()) {
            if (json.getJSONObject(s).getJSONObject("abilities").keySet().stream().map(string -> json.getJSONObject(s).getJSONObject("abilities").getString(string)).anyMatch(string -> string.equalsIgnoreCase(abi))) {
                String name = json.getJSONObject(s).getString("name");
                String[] split = name.split("-");
                if(split.length > 1) mons.add(getGerNameNoCheck(split[0]) + "-" + split[1]);
                else mons.add(getGerNameNoCheck(name));
            }
        }
        Collections.sort(mons);
        StringBuilder s = new StringBuilder();
        for (String mon : mons) {
            s.append(mon).append("\n");
            if (s.length() > 1900) {
                tco.sendMessage(new EmbedBuilder().setColor(Color.CYAN).setTitle(t.getOtherLang() + " haben:").setDescription(s).build()).queue();
                s = new StringBuilder();
            }
        }
        tco.sendMessage(new EmbedBuilder().setColor(Color.CYAN).setTitle(t.getOtherLang() + " haben:").setDescription(s).build()).queue();
        /*
        try {
            tco.sendMessage(new EmbedBuilder().setColor(Color.CYAN).setTitle(eachWordUpperCase(msg.substring(9)) + " haben:").setDescription(String.join("\n", Jsoup.connect("https://www.pokewiki.de/" + msg.substring(9)).get().select("span[style=\"padding-left: 0.2em;\"]").stream().map(Element::text).collect(Collectors.toCollection(ArrayList::new)))).build()).queue();
        } catch (IOException e) {
            e.printStackTrace();
            tco.sendMessage("Es ist ein Fehler aufgetreten!").queue();
        }*/

    }
}
