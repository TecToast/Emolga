package de.tectoast.emolga

import de.tectoast.emolga.bot.usedJDA
import de.tectoast.emolga.database.Database
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.json.Tokens
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.initMongo
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.mpp.env
import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.litote.kmongo.regex

object KotestProjectConfig : AbstractProjectConfig() {
    override val failOnEmptyTestSuite = false

    private val logger = KotlinLogging.logger {}

    override suspend fun beforeProject() {
        println("INITIALIZING")
        initMongo()
        logger.info(db.league.deleteMany(League::leaguename regex Regex("^TEST")).deletedCount.toString())
        val username = env("DBUSER")!!
        val password = env("DBPASSWORD")!!
        val host = env("DBHOST")!!
        Database.init(Tokens.Database(username, password, host), withStartUp = false)
    }

    override suspend fun afterProject() {
        logger.info("AFTER PROJECT")
        if (usedJDA) {
            logger.info("SHUTTING DOWN JDA")
            delay(3000)
        }
        logger.info("SHUTTING DOWN")
    }

}
