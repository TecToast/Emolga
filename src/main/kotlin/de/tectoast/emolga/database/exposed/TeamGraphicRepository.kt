package de.tectoast.emolga.database.exposed

import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Single

interface TeamGraphicRepository {
    suspend fun setChannelId(league: String, channelId: Long)
    suspend fun getChannelId(league: String): Long?
    suspend fun setMessageId(league: String, idx: Int, messageId: Long)
    suspend fun getMessageId(league: String, idx: Int): Long?
    suspend fun getLeagueAndIdxByMessageId(messageId: Long): Pair<String, Int>?
}

@Single
class PostgresTeamGraphicRepository(
    private val db: R2dbcDatabase,
    private val channelDB: TeamGraphicChannelDB,
    private val messageDB: TeamGraphicMessageDB
) : TeamGraphicRepository {

    override suspend fun setChannelId(league: String, channelId: Long) {
        suspendTransaction(db) {
            channelDB.upsert {
                it[channelDB.LEAGUE] = league
                it[channelDB.CHANNEL] = channelId
            }
        }
    }

    override suspend fun getChannelId(league: String) = suspendTransaction(db) {
        channelDB.select(channelDB.CHANNEL).where { channelDB.LEAGUE eq league }
            .firstOrNull()?.get(channelDB.CHANNEL)
    }

    override suspend fun setMessageId(league: String, idx: Int, messageId: Long) {
        suspendTransaction(db) {
            messageDB.upsert {
                it[messageDB.LEAGUE] = league
                it[messageDB.IDX] = idx
                it[messageDB.MESSAGEID] = messageId
            }
        }
    }

    override suspend fun getMessageId(league: String, idx: Int): Long? = suspendTransaction(db) {
        messageDB.select(messageDB.MESSAGEID)
            .where { (messageDB.LEAGUE eq league) and (messageDB.IDX eq idx) }
            .firstOrNull()?.get(messageDB.MESSAGEID)
    }

    override suspend fun getLeagueAndIdxByMessageId(messageId: Long): Pair<String, Int>? = suspendTransaction(db) {
        messageDB.select(messageDB.LEAGUE, messageDB.IDX)
            .where { messageDB.MESSAGEID eq messageId }
            .firstOrNull()?.let { it[messageDB.LEAGUE] to it[messageDB.IDX] }
    }
}
