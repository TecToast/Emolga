package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.transaction

object UsedQuestionsDB : Table("usedquestions") {
    val index = integer("index")

    fun insertIndex(index: Int) = transaction {
        insertIgnore {
            it[this.index] = index
        }
    }
}
