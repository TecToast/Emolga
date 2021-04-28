package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;

public class UpdateTierlistCommand extends Command {
    public UpdateTierlistCommand() {
        super("updatetierlist", "`!updatetierlist <Tier> <Mons> Updated die Tierliste des Servers", CommandCategory.Pokemon);
        setCustomPermissions(PermissionPreset.ADMIN);
    }

    @Override
    public void process(GuildCommandEvent e) {

    }
}
