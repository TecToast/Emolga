package de.tectoast.emolga

import de.tectoast.emolga.bot.EmolgaMain.start
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.database.Database
import org.slf4j.LoggerFactory

object Main {
    private val logger = LoggerFactory.getLogger(Main::class.java)

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        logger.info("Starting Bot...")
        Command.init()
        logger.info("Starting DB...")
        Database.init()
        logger.info("Starting EmolgaMain...")
        start()
    }
}