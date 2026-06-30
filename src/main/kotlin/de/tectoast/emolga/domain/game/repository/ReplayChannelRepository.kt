package de.tectoast.emolga.domain.game.repository


import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single


@Single
class ReplayChannelRepository(private val db: R2dbcDatabase) {
    sealed interface ReplayChannelResult {
        data object Created : ReplayChannelResult
        data class Existed(val channel: Long) : ReplayChannelResult
    }

    /**
     * Inserts a new channel replay/result combination into the database, if there is no result channel associated with the replay channel
     * @param replayChannel The replayChannel of the combination
     * @param resultChannel The resultChannel of the combination
     * @param guild the id of the guild the channels are part of
     * @return [ReplayChannelResult.Created] if the combination was created, [ReplayChannelResult.Existed] if the combination already existed
     */
    suspend fun insertChannel(replayChannel: Long, resultChannel: Long, guild: Long) = suspendTransaction(db) {
        ReplayChannelTable.selectAll().where { ReplayChannelTable.replay eq replayChannel }.firstOrNull()
            ?.get(ReplayChannelTable.result)
            ?.let { ReplayChannelResult.Existed(it) }
            ?: run {
                ReplayChannelTable.insert {
                    it[replay] = replayChannel
                    it[result] = resultChannel
                    it[this.guild] = guild
                }
                ReplayChannelResult.Created
            }
    }

    /**
     * Deletes a replay/result combination
     * @param replayChannel the replayChannel
     * @return *true* if the channel was deleted, *false* otherwise
     */
    suspend fun deleteChannel(replayChannel: Long) = suspendTransaction(db) {
        ReplayChannelTable.deleteWhere { replay eq replayChannel } != 0
    }


    /**
     * Gets all replay channels that are currently stored in the database
     * @return a list of all replay channels
     */
    suspend fun getAllReplayChannels(): List<Long> = suspendTransaction(db) {
        ReplayChannelTable.select(ReplayChannelTable.replay).map { it[ReplayChannelTable.replay] }.toList()
    }

    /**
     * Deletes all replay/result combinations for the given replay channels
     * @param replayChannels the replay channels to delete
     * @return the number of combinations deleted
     */
    suspend fun deleteChannels(replayChannels: Collection<Long>): Int = suspendTransaction(db) {
        if (replayChannels.isEmpty()) return@suspendTransaction 0
        ReplayChannelTable.deleteWhere { replay inList replayChannels }
    }

    /**
     * Gets the result channel to a specified (potential) replay channel
     * @param replayChannel the channel id to look up
     * @return the result channel id for the given channel or *null*
     */
    suspend fun getResultChannel(replayChannel: Long): Long? =
        suspendTransaction(db) {
            ReplayChannelTable.select(ReplayChannelTable.result).where { ReplayChannelTable.replay eq replayChannel }
                .firstOrNull()
                ?.get(ReplayChannelTable.result)
        }
}

object ReplayChannelTable : Table("replay_channel") {
    val replay = long("replay")
    val result = long("result")
    val guild = long("guild")

    override val primaryKey = PrimaryKey(replay)
}
