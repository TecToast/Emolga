package de.tectoast.commands.admin;

import de.tectoast.commands.Command;
import de.tectoast.commands.CommandCategory;
import de.tectoast.utils.Constants;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class AslsortCommand extends Command {
    public AslsortCommand() {
        super("aslsort", "`!aslsort` Sortiert die Tabelle der ASL", CommandCategory.Admin, Constants.ASLID);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        sortASL();
        e.getChannel().sendMessage("Done!").queue();
    }
}
