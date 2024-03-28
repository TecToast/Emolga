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
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.Statement
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
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
            CalendarDB.allEntries.forEach { CalendarSystem.scheduleCalendarEntry(it) }
            SpoilerTagsDB.addToList()
            Giveaway.init()
            EmolgaMain.updatePresence()
            BirthdaySystem.startSystem()
        }
    }
}


abstract class OnDuplicateKeyUpdate<Key : Any>(
    key: Key,
    private val map: Map<Column<*>, Any>,
    table: Table,
    checkcol: Column<*> = table.firstPrimary
) : InsertStatement<Key>(table, false) {

    init {
        values[checkcol] = key
        values.putAll(map)
    }

    override fun prepareSQL(transaction: Transaction): String {

        val onUpdateSQL = if (map.isNotEmpty()) {
            " ON DUPLICATE KEY UPDATE " + map.entries.joinToString {
                val col = transaction.identity(it.key)
                "$col=${updateUnsafe(col, it.value)}"
            }
        } else ""
        return super.prepareSQL(transaction) + onUpdateSQL
    }

    abstract fun updateUnsafe(colIdentity: String, value: Any): String
}


class Increment<Key : Any>(
    key: Key, map: Map<Column<*>, Int>, table: Table
) : OnDuplicateKeyUpdate<Key>(key, map, table) {
    override fun updateUnsafe(colIdentity: String, value: Any) = "$colIdentity+$value"
}

class Upsert<Key : Any>(
    key: Key, map: Map<Column<*>, Any>, table: Table, checkcol: Column<*> = table.firstPrimary
) : OnDuplicateKeyUpdate<Key>(key, map, table, checkcol) {
    override fun updateUnsafe(colIdentity: String, value: Any) = "${value.escaped()}}"
}

fun <T : Table, K : Any> T.increment(key: K, map: Map<Column<*>, Int>) =
    Increment(key, map, this).execute()

fun <T : Table, K : Any> T.upsert(key: K, map: Map<Column<*>, Any>, checkcol: Column<*> = firstPrimary) =
    Upsert(key, map, this, checkcol).execute()

fun Statement<*>.execute() = execute(TransactionManager.current())
private fun Any.escaped() = if (this is String) "'$this'" else this.toString()
val Table.firstPrimary get() = primaryKey!!.columns.first()
fun <T : Table> T.forAll(block: T.(ResultRow) -> Unit) = transaction {
    selectAll().forEach {
        block(it)
    }
}

fun <T> dbAsync(block: suspend CoroutineScope.() -> T) = Database.dbScope.async(start = CoroutineStart.LAZY) {
    block()
}
