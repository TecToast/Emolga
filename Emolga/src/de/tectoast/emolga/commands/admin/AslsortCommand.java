package de.tectoast.emolga.commands.admin;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.utils.CommandEvent;
import de.tectoast.emolga.utils.Constants;

public class AslsortCommand extends Command {
    public AslsortCommand() {
        super("aslsort", "`!aslsort` Sortiert die Tabelle der ASL", CommandCategory.Admin, Constants.ASLID);
    }

    @Override
    public void process(CommandEvent e) {
        sortASL();
        e.getChannel().sendMessage("Done!").queue();
    }
}
