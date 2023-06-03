package de.tectoast.emolga

import de.tectoast.emolga.bot.EmolgaMain.start
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.database.Database
import mu.KotlinLogging
import javax.crypto.BadPaddingException


private val logger = KotlinLogging.logger {}


fun main() {
    logger.info("Starting Bot...")
    val console = System.console()
    var key: String
    while (true) {
        logger.info("Enter Token Key:")
        key = String(console.readPassword())
        try {
            Command.init(key)
            break
        } catch (e: BadPaddingException) {
            logger.error("Wrong Key!")
        }
    }
    logger.info("Starting DB...")
    Database.init(Command.tokens.database, "localhost")
    logger.info("Starting EmolgaMain...")
    start()
    //logger.info("Starting KTor...")
    //Ktor.start()
}

