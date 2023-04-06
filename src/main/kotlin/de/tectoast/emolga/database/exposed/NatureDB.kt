package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object NatureDB : Table("natures") {
    val NAME = varchar("name", 30)
    val PLUS = varchar("plus", 10).nullable()
    val MINUS = varchar("minus", 10).nullable()
    private val statnames = mapOf("atk" to "Atk", "def" to "Def", "spa" to "SpAtk", "spd" to "SpDef", "spe" to "Init")

    fun getNatureData(str: String) = transaction {
        select { NAME eq str }.first().let {
            val plus = it[PLUS]
            val minus = it[MINUS]
            if (plus != null) {
                """
                ${statnames[plus]}+
                ${statnames[minus]}-
                """.trimIndent()
            } else {
                "Neutral"
            }
        }
    }
}
