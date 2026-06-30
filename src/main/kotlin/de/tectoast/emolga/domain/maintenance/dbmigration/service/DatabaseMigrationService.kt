package de.tectoast.emolga.domain.maintenance.dbmigration.service

import com.google.common.reflect.ClassPath
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.migration.r2dbc.MigrationUtils
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.isSubclassOf

@Single
class DatabaseMigrationService(private val db: R2dbcDatabase) {
    private val statementsById = ConcurrentHashMap<String, List<String>>()

    suspend fun generateMigrationPlan(): Pair<String, List<String>>? = suspendTransaction(db) {
        val statements =
            MigrationUtils.statementsRequiredForDatabaseMigration(
                *ClassPath.from(Thread.currentThread().contextClassLoader)
                    .getTopLevelClassesRecursive("de.tectoast.emolga").map { it.load().kotlin }
                    .filter { it.isSubclassOf(Table::class) }.mapNotNull { it.objectInstance as? Table? }
                    .toTypedArray(), withLogs = false)
        if (statements.isEmpty()) return@suspendTransaction null
        val id = System.currentTimeMillis().toString()
        statementsById[id] = statements
        id to statements
    }

    suspend fun executeMigration(id: String) = suspendTransaction(db) {
        val statements = statementsById[id] ?: return@suspendTransaction false
        for (sql in statements) {
            this.exec(sql)
        }
        statementsById.remove(id)
        true
    }

}
