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

interface PredictionGameMessageRepository {
    suspend fun getMessageIds(leagueName: String, gameday: Int, battle: Int? = null): List<Long>
    suspend fun setMessageId(leagueName: String, gameday: Int, battle: Int, messageId: Long)
    suspend fun deleteMessagesFromLeague(leagueName: String)

}


class PostgresPredictionGameRepository(
    val db: R2dbcDatabase,
    val messages: PredictionGameMessagesDB,
) : PredictionGameMessageRepository {

    override suspend fun getMessageIds(leagueName: String, gameday: Int, battle: Int?): List<Long> =
        suspendTransaction(db) {
            messages.select(messages.messageid).where {
                val op = (messages.leaguename eq leagueName) and (messages.week eq gameday)
                if (battle == null) op
                else op and (messages.battle eq battle)
            }.orderBy(messages.battle).map { it[messages.messageid] }.toList()
        }

    override suspend fun setMessageId(leagueName: String, gameday: Int, battle: Int, messageId: Long) {
        suspendTransaction(db) {
            messages.upsert {
                it[messages.leaguename] = leagueName
                it[messages.week] = gameday
                it[messages.battle] = battle
                it[messages.messageid] = messageId
            }
        }
    }

    override suspend fun deleteMessagesFromLeague(leagueName: String) {
        suspendTransaction(db) {
            messages.deleteWhere { messages.leaguename eq leagueName }
        }
    }
}
