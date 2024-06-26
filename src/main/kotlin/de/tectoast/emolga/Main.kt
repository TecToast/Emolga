package de.tectoast.emolga

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.database.Database
import de.tectoast.emolga.encryption.Credentials
import de.tectoast.emolga.ktor.Ktor
import de.tectoast.emolga.logging.LogConfigReload
import de.tectoast.emolga.utils.defaultScope
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.json.initMongo
import de.tectoast.emolga.utils.repeat.IntervalTask
import de.tectoast.emolga.utils.repeat.RepeatTask
import kotlinx.coroutines.launch
import mu.KotlinLogging
import javax.crypto.BadPaddingException


private val logger = KotlinLogging.logger {}


suspend fun main() {
    logger.info("Starting Bot...")
    val console = System.console()
    var key: String
    while (true) {
        logger.info("Enter Token Key:")
        key = String(console.readPassword())
        try {
            logger.info("Begin decrypt...")
            Credentials.load(key)
            break
        } catch (e: BadPaddingException) {
            logger.error("Wrong Key!")
        }
    }
    logger.info("Starting MongoDB...")
    initMongo()
    logger.info("Launching Bots...")
    EmolgaMain.launchBots()
    Tierlist.setup()
    defaultScope.launch {
        RepeatTask.setupRepeatTasks()
        IntervalTask.setupIntervalTasks()
    }
    logger.info("Starting DB...")
    Database.init(Credentials.tokens.database, "localhost")
    logger.info("Starting KTor...")
    Ktor.start()
    logger.info("Starting LogConfigReload...")
    LogConfigReload.start()
    logger.info("Starting EmolgaMain...")
    EmolgaMain.startListeners()
}
