package de.tectoast.emolga.commands.various;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;

public class PermaMoveCommand extends Command {

    public PermaMoveCommand() {
        super("permamove", "Permamove :)", CommandCategory.Various);
        setCustomPermissions(PermissionPreset.fromIDs(690971979821613056L));
    }

    @Override
    public void process(GuildCommandEvent e) {
        permaMoveGugsi = !permaMoveGugsi;
        e.reply(permaMoveGugsi ? ":D" : "D:");
    }
}
