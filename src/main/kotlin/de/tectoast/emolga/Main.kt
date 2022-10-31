package de.tectoast.emolga

import de.tectoast.emolga.bot.EmolgaMain.start
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.database.Database
import org.slf4j.LoggerFactory
import java.util.*

object Main {
    private val logger = LoggerFactory.getLogger(Main::class.java)

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        logger.info("Starting Bot...")
        val scanner = Scanner(System.`in`)
        println("Enter Token Key:")
        val key = scanner.nextLine()
        println("Enter Token IV:")
        val iv = scanner.nextLine()
        Command.init(key, iv)
        logger.info("Starting DB...")
        Database.init()
        logger.info("Starting EmolgaMain...")
        start()
    }
}
