package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object StatisticsMigrationLinks : Table("statisticsmigrationlinks") {
    val LINK = varchar("link", 255)
    val TIMESTAMP = timestamp("timestamp").defaultExpression(CurrentTimestamp)

    suspend fun add(link: String) = dbTransaction {
        insert {
            it[LINK] = link
        }
    }
}
