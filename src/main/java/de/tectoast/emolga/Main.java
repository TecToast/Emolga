package de.tectoast.emolga;

import de.tectoast.emolga.bot.EmolgaMain;
import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.database.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    public static void main(String[] args) throws Exception {
        //System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
        Logger logger = LoggerFactory.getLogger(Main.class);
        logger.info("Starting Bot...");
        Command.init();
        logger.info("Starting DB...");
        Database.init();
        logger.info("Starting EmolgaMain...");
        EmolgaMain.start();
    }

}
