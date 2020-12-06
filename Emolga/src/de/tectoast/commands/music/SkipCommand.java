package de.tectoast.commands.music;

import de.tectoast.commands.Command;
import de.tectoast.commands.CommandCategory;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.ArrayList;
import java.util.Arrays;

public class SkipCommand extends Command {
    public SkipCommand() {
        super("s", "`e!s` Skippt den derzeitigen Track", CommandCategory.Music);
        aliases.add("skip");
        overrideChannel.put("712035338846994502", new ArrayList<>(Arrays.asList("716221567079546983", "735076688144105493")));
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        skipTrack(e.getChannel());
    }
}
