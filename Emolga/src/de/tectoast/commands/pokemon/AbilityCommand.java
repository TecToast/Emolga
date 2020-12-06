package de.tectoast.commands.pokemon;

import de.tectoast.commands.Command;
import de.tectoast.commands.CommandCategory;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;

public class AbilityCommand extends Command {

    public AbilityCommand() {
        super("ability", "`!ability <Name>` Zeigt, welche Mons eine gewisse Fähigkeit haben können", CommandCategory.Pokemon);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        JSONObject json = getDataJSON();
        ArrayList<String> mons = new ArrayList<>();
        String str = getGerName(msg.substring(9));
        if (!str.split(";")[0].equals("abi")) {
            tco.sendMessage("Das ist keine Fähigkeit!").queue();
            return;
        }
        String abi = str.split(";")[1];
        for (String s : json.keySet()) {
            if (json.getJSONObject(s).getJSONObject("abilities").keySet().stream().map(string -> json.getJSONObject(s).getJSONObject("abilities").getString(string)).anyMatch(string -> string.equalsIgnoreCase(abi)))
                mons.add(json.getJSONObject(s).getString("name"));
        }
        Collections.sort(mons);
        StringBuilder s = new StringBuilder();
        for (String mon : mons) {
            s.append(mon).append("\n");
            if (s.length() > 1900) {
                tco.sendMessage(new EmbedBuilder().setColor(Color.CYAN).setTitle(abi + " haben:").setDescription(s).build()).queue();
                s = new StringBuilder();
            }
        }
        tco.sendMessage(new EmbedBuilder().setColor(Color.CYAN).setTitle(abi + " haben:").setDescription(s).build()).queue();
        /*
        try {
            tco.sendMessage(new EmbedBuilder().setColor(Color.CYAN).setTitle(eachWordUpperCase(msg.substring(9)) + " haben:").setDescription(String.join("\n", Jsoup.connect("https://www.pokewiki.de/" + msg.substring(9)).get().select("span[style=\"padding-left: 0.2em;\"]").stream().map(Element::text).collect(Collectors.toCollection(ArrayList::new)))).build()).queue();
        } catch (IOException e) {
            e.printStackTrace();
            tco.sendMessage("Es ist ein Fehler aufgetreten!").queue();
        }*/

    }
}
