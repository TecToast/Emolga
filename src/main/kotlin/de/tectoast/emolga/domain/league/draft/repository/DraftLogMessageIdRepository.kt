package de.tectoast.emolga.domain.league.draft.repository

import de.tectoast.emolga.domain.league.core.repository.referencesLeagueName
import kotlinx.coroutines.flow.associate
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Single


@Single
class DraftLogMessageIdRepository(private val db: R2dbcDatabase) {
    suspend fun getMessageIds(leagueName: String, session: Int, rounds: Collection<Int>) = suspendTransaction(db) {
        DraftLogMessageIdTable.select(DraftLogMessageIdTable.round, DraftLogMessageIdTable.messageId)
            .where { (DraftLogMessageIdTable.leagueName eq leagueName) and (DraftLogMessageIdTable.session eq session) and (DraftLogMessageIdTable.round inList rounds) }
            .associate { it[DraftLogMessageIdTable.round] to it[DraftLogMessageIdTable.messageId] }
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

object DraftLogMessageIdTable : Table("draft_log_message_ids") {
    val leagueName = text("league_name").referencesLeagueName()
    val session = integer("session")
    val round = integer("round")
    val messageId = long("message_id")

    override val primaryKey = PrimaryKey(leagueName, session, round)
}