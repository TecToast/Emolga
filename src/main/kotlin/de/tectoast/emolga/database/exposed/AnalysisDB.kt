package de.tectoast.emolga.database.exposed


import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.dbTransaction
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll

object AnalysisDB : Table("analysis") {
    val REPLAY = long("replay")
    val RESULT = long("result")
    val GUILD = long("guild")

    override val primaryKey = PrimaryKey(REPLAY)

    /**
     * Inserts a new channel replay/result combination into the database, if there is no result channel associated with the replay channel
     * @param replayChannel The replayChannel of the combination
     * @param resultChannel The resultChannel of the combination
     * @param guildId the id of the guild the channels are part of
     * @return [AnalysisResult.CREATED] if the combination was created, [AnalysisResult.Existed] if the combination already existed
     */
    suspend fun insertChannel(replayChannel: Long, resultChannel: Long, guildId: Long) = dbTransaction {
        selectAll().where { REPLAY eq replayChannel }.firstOrNull()?.get(RESULT)?.let { AnalysisResult.Existed(it) }
            ?: run {
                insert {
                    it[REPLAY] = replayChannel
                    it[RESULT] = resultChannel
                    it[GUILD] = guildId
                }
                AnalysisResult.CREATED
            }
    }

    /**
     * Deletes a replay/result combination
     * @param replayChannel the replayChannel
     * @return *true* if the channel was deleted, *false* otherwise
     */
    suspend fun deleteChannel(replayChannel: Long) = dbTransaction {
        deleteWhere { REPLAY eq replayChannel } != 0
    }

    /**
     * Removes unused replay/result combinations (that doesn't exist anymore)
     * @return the number of combinations deleted
     */
    suspend fun removeUnused() = dbTransaction {
        val unused = selectAll().filter { jda.getTextChannelById(it[REPLAY]) == null }.map { it[REPLAY] }.toList()
        deleteWhere { GUILD inList unused }
        unused.size
    }

    /**
     * Gets the result channel to a specified (potential) replay channel
     * @param tc the channel id to look up
     * @return the result channel id for the given channel or *null*
     */
    suspend fun getResultChannel(tc: Long): Long? =
        dbTransaction { selectAll().where { REPLAY eq tc }.firstOrNull()?.get(RESULT) }

    sealed interface AnalysisResult {
        data object CREATED : AnalysisResult
        data class Existed(val channel: Long) : AnalysisResult
    }
}
