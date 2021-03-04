package de.tectoast.commands.showdown;

import de.tectoast.commands.Command;
import de.tectoast.commands.CommandCategory;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONArray;
import org.json.JSONObject;

public class SpoilerTagsCommand extends Command {

    public SpoilerTagsCommand() {
        super("spoilertags", "`!spoilertags` Aktiviert oder deaktiviert den Spoilerschutz bei Showdown-Ergebnissen. (Gilt serverweit)", CommandCategory.Showdown);
        everywhere = true;
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        String gid = e.getGuild().getId();
        JSONObject json = getEmolgaJSON();
        JSONArray arr = json.getJSONArray("spoiler");
        boolean found = false;
        for (int i = 0; i < arr.length(); i++) {
            String o = arr.getString(i);
            if (o.equals(gid)) {
                arr.remove(i);
                saveEmolgaJSON();
                e.getChannel().sendMessage("Auf diesem Server sind Spoiler-Tags bei Showdown-Ergebnissen nun **deaktiviert**!").queue();
                return;
            }
        }
        arr.put(gid);
        saveEmolgaJSON();
        e.getChannel().sendMessage("Auf diesem Server sind Spoiler-Tags bei Showdown-Ergebnissen nun **aktiviert**!").queue();

    }
}
