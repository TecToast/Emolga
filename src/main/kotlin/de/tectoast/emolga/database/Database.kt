package de.tectoast.emolga.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.database.exposed.CalendarDB
import de.tectoast.emolga.database.exposed.Giveaway
import de.tectoast.emolga.database.exposed.SpoilerTagsDB
import de.tectoast.emolga.features.flegmon.BirthdaySystem
import de.tectoast.emolga.features.various.CalendarSystem
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.json.Tokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import mu.KotlinLogging

class Database(host: String, username: String, password: String) {
    val dataSource = HikariDataSource(HikariConfig().apply {
        jdbcUrl =
            "jdbc:mariadb://$host/emolga?user=$username&password=$password&minPoolSize=1&rewriteBatchedStatements=true"
    })


    companion object {
        private val logger = KotlinLogging.logger {}
        private lateinit var instance: Database
        val dbScope = createCoroutineScope("Database", Dispatchers.IO)


        suspend fun init(cred: Tokens.Database, host: String, withStartUp: Boolean = true): Database {
            logger.info("Creating DataSource...")
            instance = Database(host, cred.username, cred.password)
            org.jetbrains.exposed.sql.Database.connect(instance.dataSource)
            if (withStartUp) onStartUp()
            return instance
        }

        private suspend fun onStartUp() {
            logger.info("Retrieving all startup information...")
            CalendarDB.getAllEntries().forEach { CalendarSystem.scheduleCalendarEntry(it) }
            SpoilerTagsDB.addToList()
            Giveaway.init()
            EmolgaMain.updatePresence()
            BirthdaySystem.startSystem()
        }
    }
}

fun <T> dbAsync(block: suspend CoroutineScope.() -> T) = Database.dbScope.async(start = CoroutineStart.LAZY) {
    block()
}
