package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.upsert

object TeamGraphicMessageDB : Table("teamgraphicmessage") {
    val LEAGUE = varchar("league", 100)
    val IDX = integer("idx")
    val MESSAGEID = long("messageid")

    override val primaryKey = PrimaryKey(LEAGUE, IDX)

    suspend fun set(league: String, idx: Int, messageId: Long) = dbTransaction {
        upsert {
            it[LEAGUE] = league
            it[IDX] = idx
            it[MESSAGEID] = messageId
        }
    }

    suspend fun getMessageId(league: String, idx: Int): Long? = dbTransaction {
        select(MESSAGEID).where { (LEAGUE eq league) and (IDX eq idx) }.firstOrNull()?.get(MESSAGEID)
    }

    suspend fun getByMessageId(messageId: Long): Pair<String, Int>? = dbTransaction {
        select(LEAGUE, IDX).where { MESSAGEID eq messageId }.firstOrNull()?.let { it[LEAGUE] to it[IDX] }
    }
}