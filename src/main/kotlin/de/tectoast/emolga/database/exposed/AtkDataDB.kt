package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object AtkDataDB : Table("atkdata") {
    val NAME = varchar("name", 26)
    val DESCRIPTION = text("description")

    // generate a getData function similar to the one in AbiData.kt
    fun getData(name: String) = transaction {
        select { this@AtkDataDB.NAME eq name }.firstOrNull()?.get(DESCRIPTION)
    }

}
