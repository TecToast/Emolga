package de.tectoast.emolga

import de.tectoast.emolga.bot.EmolgaMain.start
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.database.Database
import mu.KotlinLogging


private val logger = KotlinLogging.logger {}


fun main() {
    logger.info("Starting Bot...")
    val console = System.console()
    println("Enter Token Key:")
    val key = String(console.readPassword())
    Command.init(key)
    logger.info("Starting DB...")
    Database.init(Command.tokens.database, "localhost")
    logger.info("Starting EmolgaMain...")
    start()
    //logger.info("Starting KTor...")
    //Ktor.start()
}

