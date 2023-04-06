package de.tectoast.emolga.database.exposed


import de.tectoast.emolga.bot.EmolgaMain
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object AnalysisDB : Table("analysis") {
    val REPLAY = long("replay")
    val RESULT = long("result")
    val GUILD = long("guild")

    fun insertChannel(replayChannel: Long, resultChannel: Long, guildId: Long) = transaction {
        select { REPLAY eq replayChannel }.firstOrNull()?.get(RESULT) ?: run {
            insert {
                it[REPLAY] = replayChannel
                it[RESULT] = resultChannel
                it[GUILD] = guildId
            }
            -1
        }
    }

    fun deleteChannel(replayChannel: Long) = transaction {
        deleteWhere { REPLAY eq replayChannel } != 0
    }

    fun removeUnused() = transaction {
        var x = 0
        for (row in selectAll()) {
            if (EmolgaMain.emolgajda.getTextChannelById(row[REPLAY]) == null) {
                deleteWhere { REPLAY eq row[REPLAY] }
                x++
            }
        }
        x
    }
}
