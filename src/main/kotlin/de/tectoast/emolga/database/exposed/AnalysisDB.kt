package de.tectoast.emolga.database.exposed


import de.tectoast.emolga.bot.jda
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object AnalysisDB : Table("analysis") {
    val REPLAY = long("replay")
    val RESULT = long("result")
    val GUILD = long("guild")

    suspend fun insertChannel(replayChannel: Long, resultChannel: Long, guildId: Long) = newSuspendedTransaction {
        selectAll().where { REPLAY eq replayChannel }.firstOrNull()?.get(RESULT) ?: run {
            insert {
                it[REPLAY] = replayChannel
                it[RESULT] = resultChannel
                it[GUILD] = guildId
            }
            -1
        }
    }

    suspend fun deleteChannel(replayChannel: Long) = newSuspendedTransaction {
        deleteWhere { REPLAY eq replayChannel } != 0
    }

    suspend fun removeUnused() = newSuspendedTransaction {
        var x = 0
        for (row in selectAll()) {
            if (jda.getTextChannelById(row[REPLAY]) == null) {
                deleteWhere { REPLAY eq row[REPLAY] }
                x++
            }
        }
        x
    }

    suspend fun getResultChannel(tc: Long): Long? =
        newSuspendedTransaction { selectAll().where { REPLAY eq tc }.firstOrNull()?.get(RESULT) }

}
