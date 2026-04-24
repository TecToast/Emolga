package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Single

object DraftLogMessageIdTable : Table("draft_log_message_ids") {
    val leagueName = varchar("league_name", 100)
    val session = integer("session")
    val round = integer("round")
    val messageId = long("message_id")

    override val primaryKey = PrimaryKey(leagueName, session, round)
}

@Single
class DraftLogMessageIdService(val db: R2dbcDatabase) {
    suspend fun getMessageIds(leagueName: String, session: Int, rounds: Collection<Int>) = suspendTransaction(db) {
        DraftLogMessageIdTable.select(DraftLogMessageIdTable.round, DraftLogMessageIdTable.messageId)
            .where { (DraftLogMessageIdTable.leagueName eq leagueName) and (DraftLogMessageIdTable.session eq session) and (DraftLogMessageIdTable.round inList rounds) }
            .toMap { it[DraftLogMessageIdTable.round] to it[DraftLogMessageIdTable.messageId] }
    }

    suspend fun setMessageId(leagueName: String, session: Int, round: Int, messageId: Long) = suspendTransaction(db) {
        DraftLogMessageIdTable.upsert {
            it[DraftLogMessageIdTable.leagueName] = leagueName
            it[DraftLogMessageIdTable.session] = session
            it[DraftLogMessageIdTable.round] = round
            it[DraftLogMessageIdTable.messageId] = messageId
        }
    }
}