package de.tectoast.emolga.database.exposed


import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import net.dv8tion.jda.api.JDA
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single

object AnalysisTable : Table("AnalysisTable") {
    val replay = long("replay")
    val result = long("result")
    val guild = long("guild")

    override val primaryKey = PrimaryKey(replay)
}

interface ChannelPresenceChecker {
    suspend fun doesChannelExist(channel: Long): Boolean
}

@Single
class JDAChannelPresenceChecker(val jda: JDA) : ChannelPresenceChecker {
    override suspend fun doesChannelExist(channel: Long) = jda.getTextChannelById(channel) != null
}

@Single
class AnalysisService(val repo: AnalysisRepository, val channelChecker: ChannelPresenceChecker) {
    suspend fun cleanupUnusedChannels(): Int {
        val allChannels = repo.getAllReplayChannels()
        val unusedChannels = allChannels.filterNot { channelId ->
            channelChecker.doesChannelExist(channelId)
        }
        return repo.deleteChannels(unusedChannels)
    }
}

@Single
class AnalysisRepository(val db: R2dbcDatabase) {
    sealed interface AnalysisResult {
        data object Created : AnalysisResult
        data class Existed(val channel: Long) : AnalysisResult
    }

    /**
     * Inserts a new channel replay/result combination into the database, if there is no result channel associated with the replay channel
     * @param replayChannel The replayChannel of the combination
     * @param resultChannel The resultChannel of the combination
     * @param guild the id of the guild the channels are part of
     * @return [AnalysisResult.Created] if the combination was created, [AnalysisResult.Existed] if the combination already existed
     */
    suspend fun insertChannel(replayChannel: Long, resultChannel: Long, guild: Long) = suspendTransaction(db) {
        AnalysisTable.selectAll().where { AnalysisTable.replay eq replayChannel }.firstOrNull()
            ?.get(AnalysisTable.result)
            ?.let { AnalysisResult.Existed(it) }
            ?: run {
                AnalysisTable.insert {
                    it[replay] = replayChannel
                    it[result] = resultChannel
                    it[this.guild] = guild
                }
                AnalysisResult.Created
            }
    }

    /**
     * Deletes a replay/result combination
     * @param replayChannel the replayChannel
     * @return *true* if the channel was deleted, *false* otherwise
     */
    suspend fun deleteChannel(replayChannel: Long) = suspendTransaction(db) {
        AnalysisTable.deleteWhere { replay eq replayChannel } != 0
    }


    /**
     * Removes unused replay/result combinations (that doesn't exist anymore)
     * @param check Filter to check if a replay channel is still used (eg by checking Discord)
     * @return the number of combinations deleted
     */
    suspend fun removeUnused(check: suspend (Long) -> Boolean): Int = suspendTransaction(db) {
        val unused =
            AnalysisTable.select(AnalysisTable.replay).map { it[AnalysisTable.replay] }.filter { check(it) }.toList()
        AnalysisTable.deleteWhere { replay inList unused }
        unused.size
    }

    /**
     * Gets all replay channels that are currently stored in the database
     * @return a list of all replay channels
     */
    suspend fun getAllReplayChannels(): List<Long> = suspendTransaction(db) {
        AnalysisTable.select(AnalysisTable.replay).map { it[AnalysisTable.replay] }.toList()
    }

    /**
     * Deletes all replay/result combinations for the given replay channels
     * @param replayChannels the replay channels to delete
     * @return the number of combinations deleted
     */
    suspend fun deleteChannels(replayChannels: Collection<Long>): Int = suspendTransaction(db) {
        if (replayChannels.isEmpty()) return@suspendTransaction 0
        AnalysisTable.deleteWhere { replay inList replayChannels }
    }

    /**
     * Gets the result channel to a specified (potential) replay channel
     * @param replayChannel the channel id to look up
     * @return the result channel id for the given channel or *null*
     */
    suspend fun getResultChannel(replayChannel: Long): Long? =
        suspendTransaction(db) {
            AnalysisTable.select(AnalysisTable.result).where { AnalysisTable.replay eq replayChannel }.firstOrNull()
                ?.get(AnalysisTable.result)
        }
}
