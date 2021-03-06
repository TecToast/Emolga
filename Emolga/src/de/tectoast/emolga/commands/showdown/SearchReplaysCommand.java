package de.tectoast.emolga.commands.showdown;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class SearchReplaysCommand extends Command {
    public SearchReplaysCommand() {
        super("searchreplays", "`!searchreplays <User1> [User2]` Sucht nach Replays der angegebenen Showdownbenutzernamen", CommandCategory.Showdown);
        wip = true;
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        String msg = e.getMessage().getContentDisplay().substring(15);
        String url = "https://replay.pokemonshowdown.com/search.json?user=";
        String[] split = msg.split(" ");
        if(split.length > 1) url += split[0] + "&user2=" + split[1];
        else url += split[0];
        System.out.println(url);
    }
}
