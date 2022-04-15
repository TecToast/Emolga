package de.tectoast.emolga.commands.rpi;

import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.commands.RPICommand;
import de.tectoast.emolga.utils.GPIOManager;

public class PowerOffCommand extends RPICommand {
    public PowerOffCommand() {
        super("poweroff", "Fährt den Server hart runter (5 Sekunden Power-Off)");
    }

    @Override
    public void process(GuildCommandEvent e) throws Exception {
        GPIOManager.hardStopServer();
        e.reply("Power-Off durchgeführt!");
    }
}
