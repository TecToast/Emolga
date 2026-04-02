package de.tectoast.emolga.database.exposed


import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single

interface AnalysisRepository {
    /**
     * Inserts a new channel replay/result combination into the database, if there is no result channel associated with the replay channel
     * @param replayChannel The replayChannel of the combination
     * @param resultChannel The resultChannel of the combination
     * @param guild the id of the guild the channels are part of
     * @return [AnalysisResult.Created] if the combination was created, [AnalysisResult.Existed] if the combination already existed
     */
    suspend fun insertChannel(replayChannel: Long, resultChannel: Long, guild: Long): AnalysisResult

    /**
     * Deletes a replay/result combination
     * @param replayChannel the replayChannel
     * @return *true* if the channel was deleted, *false* otherwise
     */
    suspend fun deleteChannel(replayChannel: Long): Boolean


    /**
     * Removes unused replay/result combinations (that doesn't exist anymore)
     * @param check Filter to check if a replay channel is still used (eg by checking Discord)
     * @return the number of combinations deleted
     */
    suspend fun removeUnused(check: suspend (Long) -> Boolean): Int

    /**
     * Gets the result channel to a specified (potential) replay channel
     * @param replayChannel the channel id to look up
     * @return the result channel id for the given channel or *null*
     */
    suspend fun getResultChannel(replayChannel: Long): Long?

    sealed interface AnalysisResult {
        data object Created : AnalysisResult
        data class Existed(val channel: Long) : AnalysisResult
    }
}

@Single
class AnalysisDB : Table("analysis") {
    val replay = long("replay")
    val result = long("result")
    val guild = long("guild")

    override val primaryKey = PrimaryKey(replay)
}

@Single(binds = [AnalysisRepository::class])
class PostgresAnalysisRepository(val db: R2dbcDatabase, val analysis: AnalysisDB) : AnalysisRepository {
    override suspend fun insertChannel(replayChannel: Long, resultChannel: Long, guild: Long) = suspendTransaction(db) {
        analysis.selectAll().where { analysis.replay eq replayChannel }.firstOrNull()?.get(analysis.result)
            ?.let { AnalysisRepository.AnalysisResult.Existed(it) }
            ?: run {
                analysis.insert {
                    it[replay] = replayChannel
                    it[result] = resultChannel
                    it[this.guild] = guild
                }
                AnalysisRepository.AnalysisResult.Created
            }
    }

    override suspend fun deleteChannel(replayChannel: Long) = suspendTransaction(db) {
        analysis.deleteWhere { replay eq replayChannel } != 0
    }

    override suspend fun removeUnused(check: suspend (Long) -> Boolean): Int = suspendTransaction(db) {
        val unused = analysis.select(analysis.replay).map { it[analysis.replay] }.filter { check(it) }.toList()
        analysis.deleteWhere { guild inList unused }
        unused.size
    }

    override suspend fun getResultChannel(replayChannel: Long): Long? =
        suspendTransaction(db) {
            analysis.select(analysis.result).where { analysis.replay eq replayChannel }.firstOrNull()
                ?.get(analysis.result)
        }
}
