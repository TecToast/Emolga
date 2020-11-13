package de.Flori.Commands.Admin;

import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class AslsortCommand extends Command {
    public AslsortCommand() {
        super("aslsort", "`!aslsort` Sortiert die Tabelle der ASL", CommandCategory.Admin, "518008523653775366");
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        sortASL();
        e.getChannel().sendMessage("Done!").queue();
    }
}
