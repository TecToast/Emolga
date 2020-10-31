package de.Flori.Commands.Admin;

import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class AslsortCommand extends Command {
    public AslsortCommand() {
        super("aslsort", "`!aslsort <Koko|Lele|Bulu|Fini>` Sortiert die Tabelle der Liga in der ASL", CommandCategory.Admin, "518008523653775366", "447357526997073930");
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        sortASL();
        e.getChannel().sendMessage("Done!").queue();
    }
}
