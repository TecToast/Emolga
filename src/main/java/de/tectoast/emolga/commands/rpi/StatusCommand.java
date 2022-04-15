package de.tectoast.emolga.commands.rpi;

import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.commands.RPICommand;
import de.tectoast.emolga.utils.GPIOManager;

public class StatusCommand extends RPICommand {
    public StatusCommand() {
        super("status", "Zeigt den aktuellen Status des Servers an.");
    }

    @Override
    public void process(GuildCommandEvent e) throws Exception {
        e.reply("Der Server ist %s!".formatted(GPIOManager.isOn() ? "an" : "aus"));
    }
}
