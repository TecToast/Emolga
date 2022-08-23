package de.tectoast.emolga.utils.sql.base

import de.tectoast.emolga.database.Database
import de.tectoast.emolga.utils.sql.base.columns.SQLColumn
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*
import java.util.function.Consumer
import java.util.function.Function

@Suppress("unused")
abstract class DataManager(private val tableName: String) {
    private lateinit var cols: Array<out SQLColumn<*>>
    private val ready: Boolean
        get() = this::cols.isInitialized

    fun setColumns(vararg cols: SQLColumn<*>) {
        this.cols = cols
    }

    fun forAll(c: ResultsConsumer) {
        read(selectAll()) { rc: ResultSet ->
            while (rc.next()) {
                c.consume(rc)
            }
        }
    }

    fun addStatistics(id: String, vararg defaultValues: Any) {
        addStatisticsSpecified(id, listOf(*cols), *defaultValues)
    }

    fun addStatisticsSpecified(id: String, cols: List<SQLColumn<*>>, vararg defaultValues: Any) {
        insertOrUpdate(
            (1 until cols.size).associate { cols[it] to Function<String, String> { s -> "$s+${defaultValues[it - 1]}" } },
            *mutableListOf<Any>(id).apply {
                addAll(defaultValues.toList())
            }.toTypedArray()
        )
    }

    fun replaceIfExists(vararg values: Any?) {
        insertOrUpdate(emptyMap(), *values)
    }

    private fun insertOrUpdate(map: Map<SQLColumn<*>, Function<String, String>>, vararg defaultValues: Any?) {
        insertOrUpdate(map, listOf(*cols), *defaultValues)
    }

    private fun insertOrUpdate(
        map: Map<SQLColumn<*>, Function<String, String>>,
        cols: List<SQLColumn<*>>,
        vararg defaultValues: Any?
    ) {
        try {
            connection.use { conn ->
                check(ready) { "DataManager $tableName is not initialized!" }
                val stmt = conn.createStatement()
                val q =
                    "INSERT INTO $tableName VALUES (${generateInsertString(*defaultValues)}) ON DUPLICATE KEY UPDATE ${
                        (1 until cols.size).joinToString {
                            val sc = cols[it]
                            "${sc.name}=${(map[sc] ?: Function { _ -> sc.wrap(defaultValues[it]) }).apply(sc.name)}"
                        }
                    }"
                logger.info(MarkerFactory.getMarker("important"), "q = {}", q)
                stmt.executeUpdate(q)
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }

    fun insert(vararg newValues: Any?) {
        check(ready) { "DataManager $tableName is not initialized!" }
        executeUpdate(
            "INSERT INTO $tableName VALUES (${generateInsertString(*newValues)})"
        )
    }

    private fun generateInsertString(vararg newValues: Any?): String {
        val l: MutableList<String> = ArrayList()
        for (i in cols.indices) {
            val newValue = newValues[i]
            l.add(newValue?.let { cols[i].wrap(it) } ?: "NULL")
        }
        return java.lang.String.join(", ", l)
    }

    private fun deleteStmt(where: String): String {
        return "DELETE FROM $tableName WHERE $where"
    }

    fun delete(where: String): Int {
        return executeUpdate(deleteStmt(where))
    }

    fun select(where: String?, vararg cols: SQLColumn<*>): String {
        return select(where, cols.joinToString { s: SQLColumn<*> -> s.name })
    }

    fun selectAll(where: String?): String {
        return select(where, "*")
    }

    fun selectAll(): String {
        return select(null, "*")
    }

    fun select(where: String?, cols: String): String {
        return "SELECT " + cols + " FROM " + tableName + if (where == null) "" else " WHERE $where"
    }

    class SelectBuilder {
        private var cols: String? = null
        private var where: String? = null
        private var orderBy: String? = null
        fun cols(vararg cols: SQLColumn<*>): SelectBuilder {
            this.cols = cols.joinToString { s: SQLColumn<*> -> s.name }
            return this
        }

        fun count(alias: String): SelectBuilder {
            cols = "count(*) as $alias"
            return this
        }

        fun where(where: String?): SelectBuilder {
            this.where = where
            return this
        }

        fun orderBy(orderBy: SQLColumn<*>, direction: String): SelectBuilder {
            this.orderBy = orderBy.name + " " + direction
            return this
        }

        fun build(dm: DataManager): String {
            return "SELECT ${cols ?: "*"} FROM ${dm.tableName}${where?.let { " WHERE $it" } ?: ""}${orderBy?.let { " ORDER BY $it" } ?: ""}"
        }
    }

    fun interface ResultsConsumer {
        @Throws(SQLException::class)
        fun consume(results: ResultSet)
    }

    fun interface ResultsFunction<T> {
        @Throws(SQLException::class)
        fun apply(results: ResultSet): T
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DataManager::class.java)
        fun executeUpdate(query: String): Int {
            try {
                connection.use { conn ->
                    val statement = conn.createStatement()
                    if (query.startsWith("INSERT INTO") || query.startsWith("UPDATE") || query.startsWith("DELETE")) {
                        logger.info("query = $query")
                    }
                    return statement.executeUpdate(query)
                }
            } catch (throwables: SQLException) {
                throwables.printStackTrace()
                return -1
            }
        }


        @JvmStatic
        protected fun selectBuilder(): SelectBuilder {
            return SelectBuilder()
        }

        protected val connection: Connection
            get() = Database.connection

        fun read(query: String?, rc: ResultsConsumer) {
            try {
                connection.use { conn ->
                    val statement = conn.createStatement()
                    val results = statement.executeQuery(query)
                    rc.consume(results)
                    statement.close()
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }

        fun <T> read(query: String?, rf: ResultsFunction<T>): T {
            try {
                connection.use { conn ->
                    val statement = conn.createStatement()
                    logger.info(query)
                    val results = statement.executeQuery(query)
                    return rf.apply(results)
                }
            } catch (e: SQLException) {
                throw e
            }
        }

        fun readWrite(query: String?, rc: ResultsConsumer) {
            try {
                connection.use { conn ->
                    val statement = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)
                    val results = statement.executeQuery(query)
                    rc.consume(results)
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }


        fun <T> readWrite(query: String?, rf: ResultsFunction<T>): T {
            try {
                connection.use { conn ->
                    val set = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)
                        .executeQuery(query)
                    return rf.apply(set)
                }
            } catch (e: SQLException) {
                throw e
            }
        }


        fun <T> map(set: ResultSet, mapper: Function<ResultSet, T>): List<T> {
            val l: MutableList<T> = LinkedList()
            try {
                while (set.next()) {
                    l.add(mapper.apply(set))
                }
                set.close()
            } catch (ex: SQLException) {
                ex.printStackTrace()
            }
            return l
        }


        fun <T> mapFirst(set: ResultSet, mapper: Function<ResultSet, T>, orElse: T): T {
            try {
                if (set.next()) {
                    val `val` = mapper.apply(set)
                    set.close()
                    return `val`
                }
            } catch (ex: SQLException) {
                ex.printStackTrace()
            }
            return orElse
        }


        fun <T> mapFirst(set: ResultSet, mapper: Function<ResultSet, T>): T {
            try {
                if (set.next()) {
                    val `val` = mapper.apply(set)
                    set.close()
                    return `val`
                }
            } catch (ex: SQLException) {
                throw ex
            }
            throw RuntimeException("MapFirst returns null")
        }


        fun unwrapCount(set: ResultSet, alias: String): Int? {
            try {
                return set.getInt(alias)
            } catch (e: SQLException) {
                e.printStackTrace()
            }
            return null
        }


        fun forEach(set: ResultSet, consumer: Consumer<ResultSet>) {
            try {
                while (set.next()) {
                    consumer.accept(set)
                }
                set.close()
            } catch (ex: SQLException) {
                ex.printStackTrace()
            }
        }
    }
}