package de.tectoast.emolga.commands.rpi;

import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.commands.RPICommand;
import de.tectoast.emolga.utils.GPIOManager;

public class StartServerCommand extends RPICommand {


    public StartServerCommand() {
        super("startserver", "Startet den Server");
    }

    @Override
    public void process(GuildCommandEvent e) throws Exception {
        GPIOManager.startServer();
        e.reply("Der Server wurde gestartet!");
    }
}
