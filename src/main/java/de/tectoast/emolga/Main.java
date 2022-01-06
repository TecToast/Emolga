package de.tectoast.emolga;

import de.tectoast.emolga.bot.EmolgaMain;
import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.database.Database;

public class Main {

    public static void main(String[] args) throws Exception {
        //System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
        System.out.println("Starting Bot...");
        Command.init();
        System.out.println("Starting DB...");
        Database.init();
        System.out.println("Starting EmolgaMain...");
        EmolgaMain.start();
    }

}
