package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.max

object StatisticsDB : Table("statistics") {
    val TIMESTAMP = timestamp("timestamp")
    val META = enumeration<StatisticsMeta>("meta")
    val VALUE = integer("value")

    suspend inline fun <T> getAllAnalysis(crossinline mapper: StatisticsDB.(ResultRow) -> T): List<T> = dbTransaction {
        select(TIMESTAMP, VALUE).where { META eq StatisticsMeta.ANALYSIS }.map { mapper(it) }
    }

    suspend fun getCurrentState(meta: StatisticsMeta): Int = dbTransaction {
        val max = VALUE.max()
        select(max).where { META eq meta }.firstOrNull()?.get(max) ?: 0
    }

    suspend fun increment(meta: StatisticsMeta) = dbTransaction {
        val newValue = getCurrentState(meta) + 1
        insertIgnore {
            it[TIMESTAMP] = Clock.System.now()
            it[META] = meta
            it[VALUE] = newValue
        }
    }
}

enum class StatisticsMeta {
    ANALYSIS
}