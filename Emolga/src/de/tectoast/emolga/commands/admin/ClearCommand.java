package de.tectoast.emolga.commands.admin;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;

import java.util.stream.Collectors;

public class ClearCommand extends Command {
    public ClearCommand() {
        super("clear", "Cleart den Channel", CommandCategory.Flo);
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        e.getChannel().deleteMessages(e.getChannel().getIterableHistory().stream().collect(Collectors.toList())).queue();
    }
}
