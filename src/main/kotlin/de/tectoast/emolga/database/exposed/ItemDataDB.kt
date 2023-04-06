package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object ItemDataDB : Table("itemdata") {
    val NAME = varchar("name", 30)
    val DESCRIPTION = text("description")

    fun getData(itemname: String) = transaction {
        select { NAME eq itemname }.firstOrNull()?.get(DESCRIPTION)
    }
}
