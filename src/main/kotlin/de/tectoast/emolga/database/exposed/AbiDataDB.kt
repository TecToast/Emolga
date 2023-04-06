package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object AbiDataDB : Table("abidata") {
    val NAME = varchar("name", 16)
    val DESCRIPTION = text("description")

    fun getData(name: String) = transaction {
        select { this@AbiDataDB.NAME eq name }.firstOrNull()?.get(DESCRIPTION)
    }
}
