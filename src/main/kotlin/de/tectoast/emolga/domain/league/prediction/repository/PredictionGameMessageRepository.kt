package de.tectoast.emolga.domain.league.prediction.repository

import de.tectoast.emolga.domain.league.core.repository.referencesLeagueName
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Single

@Single
class PredictionGameMessageRepository(
    private val db: R2dbcDatabase,
) {

    suspend fun getMessageIds(leagueName: String, week: Int, battle: Int? = null): List<Long> =
        suspendTransaction(db) {
            PredictionGameMessagesTable.select(PredictionGameMessagesTable.messageid).where {
                val op =
                    (PredictionGameMessagesTable.leaguename eq leagueName) and (PredictionGameMessagesTable.week eq week)
                if (battle == null) op
                else op and (PredictionGameMessagesTable.battle eq battle)
            }.orderBy(PredictionGameMessagesTable.battle).map { it[PredictionGameMessagesTable.messageid] }.toList()
        }

    suspend fun setMessageId(leagueName: String, week: Int, battle: Int, messageId: Long) {
        suspendTransaction(db) {
            PredictionGameMessagesTable.upsert {
                it[PredictionGameMessagesTable.leaguename] = leagueName
                it[PredictionGameMessagesTable.week] = week
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

object PredictionGameMessagesTable : Table("predictiongamemessages") {
    val leaguename = text("leaguename").referencesLeagueName()
    val week = integer("week")
    val battle = integer("battle")
    val messageid = long("messageid")

    override val primaryKey = PrimaryKey(leaguename, week, battle)
}

object PredictionGameVotesTable : Table("predictiongamevotes") {
    val leagueName = text("leaguename").referencesLeagueName()
    val userId = long("userid")
    val week = integer("week")
    val battle = integer("battle")
    val idx = integer("idx")
    val correct = bool("correct").nullable().default(null)

    override val primaryKey = PrimaryKey(leagueName, userId, week, battle)
}
