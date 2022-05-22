package de.tectoast.emolga.commands.soullink;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;

public class UpdateSoullinkCommand extends Command {

    public UpdateSoullinkCommand() {
        super("updatesoullink", "Updated die Message", CommandCategory.Soullink);
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
        slash(true, 695943416789598208L);
    }

    @Override
    public void process(GuildCommandEvent e) {
        updateSoullink();
        e.reply("Done!", true);
    }
}
