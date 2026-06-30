package de.tectoast.emolga.domain.scheduling.interval.repository

import de.tectoast.emolga.domain.scheduling.interval.model.IntervalTaskData
import de.tectoast.emolga.domain.scheduling.interval.model.IntervalTaskKey
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Single
import kotlin.time.Instant


@Single
class IntervalTaskRepository(private val db: R2dbcDatabase) {
    suspend fun getTask(key: IntervalTaskKey): IntervalTaskData? = suspendTransaction(db) {
        IntervalTasksTable.select(IntervalTasksTable.nextExecution, IntervalTasksTable.notAfter)
            .where { IntervalTasksTable.key eq key.value }
            .firstOrNull()?.toData()
    }

    suspend fun upsertTask(key: IntervalTaskKey, nextExecution: Instant) = suspendTransaction(db) {
        IntervalTasksTable.upsert {
            it[this.key] = key.value
            it[this.nextExecution] = nextExecution
        }
    }

    private fun ResultRow.toData(): IntervalTaskData {
        return IntervalTaskData(this[IntervalTasksTable.nextExecution], this[IntervalTasksTable.notAfter])
    }
}


object IntervalTasksTable : Table("interval_tasks") {
    val key = text("key")
    val nextExecution = timestamp("next_execution")
    val notAfter = timestamp("not_after").nullable()

    override val primaryKey = PrimaryKey(key)
}
