package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert

object StatisticsMigrationLinks : Table("statisticsmigrationlinks") {
    val LINK = varchar("link", 255)

    suspend fun add(link: String) = dbTransaction {
        insert {
            it[LINK] = link
        }
    }
}