package de.tectoast.emolga.commands.admin;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.utils.Constants;
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
