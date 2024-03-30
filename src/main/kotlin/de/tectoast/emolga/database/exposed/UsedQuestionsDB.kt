package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object UsedQuestionsDB : Table("usedquestions") {
    val index = integer("index")

    suspend fun insertIndex(index: Int) = newSuspendedTransaction {
        insertIgnore {
            it[this.index] = index
        }
    }
}
