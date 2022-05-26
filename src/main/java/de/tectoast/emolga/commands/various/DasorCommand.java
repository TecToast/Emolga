package de.tectoast.emolga.commands.various;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.sql.DBManagers;

public class DasorCommand extends Command {


    public DasorCommand() {
        super("dasor", "ist cool", CommandCategory.Various);
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) throws Exception {
        e.reply(DBManagers.DASOR_USAGE.buildMessage());
    }
}
