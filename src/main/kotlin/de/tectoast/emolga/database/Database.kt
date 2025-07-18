package de.tectoast.emolga.database

import de.tectoast.emolga.database.exposed.CalendarDB
import de.tectoast.emolga.features.flegmon.BirthdaySystem
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.json.Tokens
import io.r2dbc.spi.ConnectionFactoryOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import mu.KotlinLogging
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

class Database(private val host: String, private val username: String, private val password: String) {


    companion object {
        private val logger = KotlinLogging.logger {}
        private lateinit var instance: Database
        val dbScope = createCoroutineScope("Database", Dispatchers.IO)


        /**
         * Initializes the database connection
         */
        suspend fun init(cred: Tokens.Database, withStartUp: Boolean = true): Database {
            logger.info("Creating DataSource...")
            instance = Database(cred.host, cred.username, cred.password)
            org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase.connect("r2dbc:mariadb://${cred.host}/emolga") {
                connectionFactoryOptions {
                    option(ConnectionFactoryOptions.USER, cred.username)
                    option(ConnectionFactoryOptions.PASSWORD, cred.password)
                }
            }
            if (withStartUp) onStartUp()
            return instance
        }

        /**
         * Calls all start up initialization function of the different features
         */
        private suspend fun onStartUp() {
            logger.info("Retrieving all startup information...")
            CalendarDB.init()
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
    suspendTransaction(Database.dbScope.coroutineContext, statement = statement)