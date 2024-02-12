package de.tectoast.emolga

import de.tectoast.emolga.bot.usedJDA
import de.tectoast.emolga.database.Database
import de.tectoast.emolga.utils.json.Tokens
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.initMongo
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.mpp.env
import kotlinx.coroutines.delay
import org.litote.kmongo.regex

object KotestProjectConfig : AbstractProjectConfig() {
    override val failOnEmptyTestSuite = false

    override suspend fun beforeProject() {
        initMongo()
        println(db.drafts.deleteMany(League::leaguename regex Regex("^TEST")).deletedCount)
        val username = env("DBUSER")!!
        val password = env("DBPASSWORD")!!
        val host = env("DBHOST")!!
        Database.init(Tokens.Database(username, password), host, withStartUp = false)
    }

    override suspend fun afterProject() {
        println("AFTER PROJECT")
        if (usedJDA) {
            println("SHUTTING DOWN JDA")
            delay(3000)
        }
        println("SHUTTING DOWN")
    }

}
