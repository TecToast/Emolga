package de.tectoast.emolga.commands.admin;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.stream.Collectors;

public class ClearCommand extends Command {
    public ClearCommand() {
        super("clear", "`!clear` Cleart den Channel", CommandCategory.Flo);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        e.getChannel().deleteMessages(e.getChannel().getIterableHistory().stream().collect(Collectors.toList())).queue();
    }
}
