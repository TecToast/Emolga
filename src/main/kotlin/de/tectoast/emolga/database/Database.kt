package de.tectoast.emolga.database

import de.tectoast.emolga.database.exposed.CalendarDB
import de.tectoast.emolga.features.flegmon.BirthdaySystem
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.json.Tokens
import io.r2dbc.spi.ConnectionFactoryOptions.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import mu.KotlinLogging
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import java.sql.SQLIntegrityConstraintViolationException

class Database(
    private val cred: Tokens.Database
) {
    val db = R2dbcDatabase.connect {
        connectionFactoryOptions {
            option(DRIVER, "pool")
            option(
                PROTOCOL,
                "postgresql"
            ) // driver identifier, PROTOCOL is delegated as DRIVER by the pool.
            option(HOST, cred.host)
            option(PORT, cred.port)
            option(USER, cred.username)
            option(PASSWORD, cred.password)
            option(DATABASE, "emolga")
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
        lateinit var instance: Database
        val dbScope = createCoroutineScope("Database", Dispatchers.IO)


        /**
         * Initializes the database connection
         */
        suspend fun init(cred: Tokens.Database, withStartUp: Boolean = true): Database {
            logger.info("Creating DataSource...")
            instance = Database(cred)
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
 * @return Deferred of the result of the block
 */
fun <T> dbAsync(block: suspend CoroutineScope.() -> T) = Database.dbScope.async(start = CoroutineStart.LAZY) {
    block()
}

suspend fun <T> dbTransaction(statement: suspend Transaction.() -> T) =
    suspendTransaction { statement() }


suspend fun dbTransactionWithUniqueHandler(
    statement: suspend Transaction.() -> Unit,
    uniqueHandler: suspend () -> Unit
) {
    try {
        dbTransaction { statement() }
    } catch (e: ExposedSQLException) {
        if (e.cause !is SQLIntegrityConstraintViolationException) throw e
        uniqueHandler()
    }
}
