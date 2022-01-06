package de.tectoast.emolga.commands.showdown;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jsolf.JSONArray;
import org.jsolf.JSONObject;
import org.jsolf.JSONTokener;

import java.io.IOException;
import java.net.URL;

public class SearchReplaysCommand extends Command {
    public SearchReplaysCommand() {
        super("searchreplays", "Sucht nach Replays der angegebenen Showdownbenutzernamen", CommandCategory.Showdown);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("user1", "User 1", "Der Showdown-Username von jemandem", ArgumentManagerTemplate.Text.any())
                .add("user2", "User 2", "Der Showdown-Username eines potenziellen zweiten Users", ArgumentManagerTemplate.Text.any(), true)
                .setExample("!searchreplays TecToast")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) throws IOException {
        TextChannel tco = e.getChannel();
        String url = "https://replay.pokemonshowdown.com/search.json?user=";
        ArgumentManager args = e.getArguments();
        String user1 = args.getText("user1");
        if (args.has("user2")) url += toSDName(user1) + "&user2=" + toSDName(args.getText("user2"));
        else url += toSDName(user1);
        System.out.println(url);
        JSONArray array = new JSONArray(new JSONTokener(new URL(url).openStream()));
        System.out.println(array.toString(4));
        StringBuilder str = new StringBuilder();
        if (array.length() == 0) {
            if (args.has("user2"))
                e.reply("Es wurde kein Kampf zwischen " + user1 + " und " + args.getText("user2") + " hochgeladen!");
            else
                e.reply("Es wurde kein Kampf von " + user1 + " hochgeladen!");
            return;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.getJSONObject(i);
            str.append(o.getString("p1")).append(" vs ").append(o.getString("p2")).append(": https://replay.pokemonshowdown.com/").append(o.getString("id")).append("\n");
        }
        System.out.println(str);
        e.getChannel().sendMessage(str.toString()).queue();
    }
}
