package de.Flori.Commands.Music;

import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

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
