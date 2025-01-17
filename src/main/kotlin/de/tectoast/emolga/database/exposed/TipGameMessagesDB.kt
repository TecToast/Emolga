package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.upsert

object TipGameMessagesDB : Table("tipgamemessages") {
    val LEAGUENAME = varchar("leaguename", 31)
    val GAMEDAY = integer("gameday")
    val BATTLE = integer("battle")
    val MESSAGEID = long("messageid")

    suspend fun get(leagueName: String, gameDay: Int, battle: Int? = null) = newSuspendedTransaction {
        select(MESSAGEID).where {
            val op = (LEAGUENAME eq leagueName) and (GAMEDAY eq gameDay)
            if (battle == null) op
            else op and (BATTLE eq battle)
        }.map { it[MESSAGEID] }
    }

    suspend fun set(leagueName: String, gameDay: Int, battle: Int, messageId: Long) = newSuspendedTransaction {
        upsert {
            it[LEAGUENAME] = leagueName
            it[GAMEDAY] = gameDay
            it[BATTLE] = battle
            it[MESSAGEID] = messageId
        }
    }

}