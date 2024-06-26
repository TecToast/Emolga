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
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.Statement
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class Database(host: String, username: String, password: String) {
    val dataSource: DataSource

    init {
        //dataSource = new MariaDbPoolDataSource("jdbc:mariadb://localhost/emolga?user=%s&password=%s&minPoolSize=1".formatted(username, password));
        val conf = HikariConfig()
        conf.jdbcUrl =
            "jdbc:mariadb://$host/emolga?user=$username&password=$password&minPoolSize=1&rewriteBatchedStatements=true"
        dataSource = HikariDataSource(conf)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Database::class.java)
        private lateinit var instance: Database
        val dbScope =
            createCoroutineScope("Database", Dispatchers.IO)


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

fun Statement<*>.execute() = execute(TransactionManager.current())
private fun Any.escaped() = if (this is String) "'$this'" else this.toString()
val Table.firstPrimary get() = primaryKey!!.columns.first()
suspend fun <T : Table> T.forAll(block: T.(ResultRow) -> Unit) = newSuspendedTransaction {
    selectAll().forEach {
        block(it)
    }
}

fun <T> dbAsync(block: suspend CoroutineScope.() -> T) = Database.dbScope.async(start = CoroutineStart.LAZY) {
    block()
}
