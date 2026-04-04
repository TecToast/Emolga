package de.tectoast.emolga.database.exposed

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.upsert

class PredictionGameMessageRepository(
    val db: R2dbcDatabase,
) {

    suspend fun getMessageIds(leagueName: String, gameday: Int, battle: Int? = null): List<Long> =
        suspendTransaction(db) {
            PredictionGameMessagesTable.select(PredictionGameMessagesTable.messageid).where {
                val op =
                    (PredictionGameMessagesTable.leaguename eq leagueName) and (PredictionGameMessagesTable.week eq gameday)
                if (battle == null) op
                else op and (PredictionGameMessagesTable.battle eq battle)
            }.orderBy(PredictionGameMessagesTable.battle).map { it[PredictionGameMessagesTable.messageid] }.toList()
        }

    suspend fun setMessageId(leagueName: String, gameday: Int, battle: Int, messageId: Long) {
        suspendTransaction(db) {
            PredictionGameMessagesTable.upsert {
                it[PredictionGameMessagesTable.leaguename] = leagueName
                it[PredictionGameMessagesTable.week] = gameday
                it[PredictionGameMessagesTable.battle] = battle
                it[PredictionGameMessagesTable.messageid] = messageId
            }
        }
    }

    suspend fun deleteMessagesFromLeague(leagueName: String) {
        suspendTransaction(db) {
            PredictionGameMessagesTable.deleteWhere { PredictionGameMessagesTable.leaguename eq leagueName }
        }
    }
}
