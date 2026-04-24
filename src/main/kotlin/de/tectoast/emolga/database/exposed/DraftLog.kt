package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.league.DraftLogEntry
import de.tectoast.emolga.database.league.PreparedDraftLogEntry
import de.tectoast.emolga.utils.groupByMapping
import de.tectoast.emolga.utils.jsonb
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.batchInsert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update

object DraftLogTable : Table("draft_log") {
    val id = integer("id").autoIncrement()
    val leagueName = varchar("league_name", 255)
    val session = integer("session")
    val round = integer("round")
    val idx = integer("idx")
    val data = jsonb<DraftLogEntry>("data")

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, leagueName, session)
    }
}

class DraftLogRepository(val db: R2dbcDatabase) {
    suspend fun insertLogEntries(leagueName: String, session: Int, data: List<PreparedDraftLogEntry>) =
        suspendTransaction(db) {
            DraftLogTable.batchInsert(data) {
                this[DraftLogTable.leagueName] = leagueName
                this[DraftLogTable.session] = session
                this[DraftLogTable.round] = it.round
                this[DraftLogTable.idx] = it.idx
                this[DraftLogTable.data] = it.entry
            }
        }

    suspend fun setMadeUpRound(leagueName: String, session: Int, round: Int, idx: Int, madeUpRound: Int) =
        suspendTransaction(db) {
            val (logId, data) = DraftLogTable.select(DraftLogTable.data)
                .where { (DraftLogTable.leagueName eq leagueName) and (DraftLogTable.session eq session) and (DraftLogTable.round eq round) and (DraftLogTable.idx eq idx) }
                .orderBy(DraftLogTable.id).mapNotNull {
                    val id = it[DraftLogTable.id]
                    val data = it[DraftLogTable.data]
                    if (data !is DraftLogEntry.Skip) return@mapNotNull null
                    if (data.madeUpRound != null) return@mapNotNull null
                    id to data
                }.firstOrNull() ?: return@suspendTransaction
            DraftLogTable.update({ DraftLogTable.id eq logId }) {
                it[DraftLogTable.data] = data.copy(madeUpRound = madeUpRound)
            }
        }

    suspend fun getLogEntriesForRounds(leagueName: String, session: Int, modifiedRounds: Collection<Int>) =
        suspendTransaction(db) {
            DraftLogTable.select(DraftLogTable.round, DraftLogTable.idx, DraftLogTable.data)
                .where { (DraftLogTable.leagueName eq leagueName) and (DraftLogTable.session eq session) and (DraftLogTable.round inList modifiedRounds) }
                .orderBy(DraftLogTable.id).groupByMapping({ it[DraftLogTable.round] }) {
                    PreparedDraftLogEntry(
                        round = it[DraftLogTable.round],
                        idx = it[DraftLogTable.idx],
                        entry = it[DraftLogTable.data]
                    )
                }
        }
}