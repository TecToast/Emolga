package de.Flori.Commands.Admin;

import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class ClearCommand extends Command {
    @Override
    public void process(GuildMessageReceivedEvent e) {
        e.getChannel().deleteMessages(e.getChannel().getIterableHistory().stream().collect(Collectors.toList())).queue();
    }

    public ClearCommand() {
        super("clear", "`!clear` Cleart den Channel", CommandCategory.Flo);
    }
}
