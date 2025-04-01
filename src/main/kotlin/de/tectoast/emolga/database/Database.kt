package de.tectoast.emolga.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.database.exposed.CalendarDB
import de.tectoast.emolga.database.exposed.Giveaway
import de.tectoast.emolga.features.flegmon.BirthdaySystem
import de.tectoast.emolga.features.various.CalendarSystem
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.json.Tokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class Database(host: String, username: String, password: String) {
    val dataSource = HikariDataSource(HikariConfig().apply {
        jdbcUrl =
            "jdbc:mariadb://$host/emolga?user=$username&password=$password&minPoolSize=1&rewriteBatchedStatements=true"
    })


    companion object {
        private val logger = KotlinLogging.logger {}
        private lateinit var instance: Database
        val dbScope = createCoroutineScope("Database", Dispatchers.IO)
        // TODO: use this scope for all database operations


        /**
         * Initializes the database connection
         */
        suspend fun init(cred: Tokens.Database, host: String, withStartUp: Boolean = true): Database {
            logger.info("Creating DataSource...")
            instance = Database(host, cred.username, cred.password)
            org.jetbrains.exposed.sql.Database.connect(instance.dataSource)
            if (withStartUp) onStartUp()
            return instance
        }

        /**
         * Calls all start up initialization function of the different features
         */
        private suspend fun onStartUp() {
            logger.info("Retrieving all startup information...")
            CalendarDB.getAllEntries().forEach { CalendarSystem.scheduleCalendarEntry(it) }
            Giveaway.init()
            EmolgaMain.updatePresence()
            BirthdaySystem.startSystem()
        }
    }
}

/**
 * Executes a given block as async in the [Database.dbScope]
 * @param block the block to execute
 * @return a Deferred of the result of the block
 */
fun <T> dbAsync(block: suspend CoroutineScope.() -> T) = Database.dbScope.async(start = CoroutineStart.LAZY) {
    block()
}

suspend fun <T> dbTransaction(statement: suspend Transaction.() -> T) =
    newSuspendedTransaction(Database.dbScope.coroutineContext, statement = statement)