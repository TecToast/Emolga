package de.tectoast.emolga.commands.showdown;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.TextChannel;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.net.URL;

public class SearchReplaysCommand extends Command {
    public SearchReplaysCommand() {
        super("searchreplays", "`!searchreplays <User1> [User2]` Sucht nach Replays der angegebenen Showdownbenutzernamen", CommandCategory.Showdown);
    }

    @Override
    public void process(GuildCommandEvent e) throws IOException {
        TextChannel tco = e.getChannel();
        String msg = e.getMessage().getContentDisplay().substring(15);
        String url = "https://replay.pokemonshowdown.com/search.json?user=";
        String[] split = msg.split(" ");
        if(split.length > 1) url += split[0].toLowerCase() + "&user2=" + split[1].toLowerCase();
        else url += split[0];
        System.out.println(url);
        JSONArray array = new JSONArray(new JSONTokener(new URL(url).openStream()));
        System.out.println(array.toString(4));
        StringBuilder str = new StringBuilder();
        if(array.length() == 0) {
            tco.sendMessage("Es wurde kein Kampf zwischen " + split[0] + " und " + split[1] + " hochgeladen!").queue();
            return;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.getJSONObject(i);
            str.append(o.getString("p1")).append(" vs ").append(o.getString("p2")).append(": https://replay.pokemonshowdown.com/").append(o.getString("id")).append("\n");
        }
        System.out.println(str.toString());

        e.getChannel().sendMessage(str.toString()).queue();
    }
}
