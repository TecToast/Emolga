package de.tectoast.emolga.database.exposed

import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Single

@Single
class TeamGraphicRepository(
    private val db: R2dbcDatabase,
) {

    suspend fun setChannelId(league: String, channelId: Long) {
        suspendTransaction(db) {
            TeamGraphicChannelTable.upsert {
                it[TeamGraphicChannelTable.league] = league
                it[TeamGraphicChannelTable.channel] = channelId
            }
        }
    }

    suspend fun getChannelId(league: String) = suspendTransaction(db) {
        TeamGraphicChannelTable.select(TeamGraphicChannelTable.channel)
            .where { TeamGraphicChannelTable.league eq league }
            .firstOrNull()?.get(TeamGraphicChannelTable.channel)
    }

    suspend fun setMessageId(league: String, idx: Int, messageId: Long) {
        suspendTransaction(db) {
            TeamGraphicMessageTable.upsert {
                it[TeamGraphicMessageTable.league] = league
                it[TeamGraphicMessageTable.idx] = idx
                it[TeamGraphicMessageTable.messageId] = messageId
            }
        }
    }

    suspend fun getMessageId(league: String, idx: Int): Long? = suspendTransaction(db) {
        TeamGraphicMessageTable.select(TeamGraphicMessageTable.messageId)
            .where { (TeamGraphicMessageTable.league eq league) and (TeamGraphicMessageTable.idx eq idx) }
            .firstOrNull()?.get(TeamGraphicMessageTable.messageId)
    }

    suspend fun getLeagueAndIdxByMessageId(messageId: Long): Pair<String, Int>? = suspendTransaction(db) {
        TeamGraphicMessageTable.select(TeamGraphicMessageTable.league, TeamGraphicMessageTable.idx)
            .where { TeamGraphicMessageTable.messageId eq messageId }
            .firstOrNull()?.let { it[TeamGraphicMessageTable.league] to it[TeamGraphicMessageTable.idx] }
    }
}
