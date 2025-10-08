package de.tectoast.emolga.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import de.tectoast.emolga.database.exposed.CalendarDB
import de.tectoast.emolga.features.flegmon.BirthdaySystem
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.json.Tokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import mu.KotlinLogging
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import java.sql.SQLIntegrityConstraintViolationException

class Database(private val host: String, private val username: String, private val password: String) {
    val dataSource = HikariDataSource(HikariConfig().apply {
        jdbcUrl =
            "jdbc:mariadb://$host/emolga?user=${this@Database.username}&password=${this@Database.password}&minPoolSize=1&rewriteBatchedStatements=true"
    })

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
            org.jetbrains.exposed.v1.jdbc.Database.connect(instance.dataSource)
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

@Suppress("DEPRECATION") // Switch to R2DBC once it is stable with pooling
suspend fun <T> dbTransaction(statement: suspend Transaction.() -> T) =
    newSuspendedTransaction(Database.dbScope.coroutineContext, statement = statement)


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
