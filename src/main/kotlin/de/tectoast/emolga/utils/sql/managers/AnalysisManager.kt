package de.tectoast.emolga.utils.sql.managers

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.utils.sql.base.DataManager
import de.tectoast.emolga.utils.sql.base.columns.LongColumn
import net.dv8tion.jda.api.entities.TextChannel
import java.sql.ResultSet

object AnalysisManager : DataManager("analysis") {
    private val REPLAY = LongColumn("replay", this)
    private val RESULT = LongColumn("result", this)
    private val GUILD = LongColumn("guild", this)

    init {
        setColumns(REPLAY, RESULT, GUILD)
    }

    fun insertChannel(replayChannel: TextChannel, resultChannel: Long): Long {
        val l = RESULT.retrieveValue(REPLAY, replayChannel.idLong)
        if (l != null) {
            return l
        }
        insert(replayChannel.idLong, resultChannel, replayChannel.guild.idLong)
        return -1
    }

    fun deleteChannel(replayChannel: Long): Boolean {
        return delete(REPLAY.check(replayChannel)) > 0
    }

    fun removeUnused(): Int {
        return readWrite<Int>(selectAll()) { r: ResultSet ->
            var x = 0
            while (r.next()) {
                if (EmolgaMain.emolgajda.getTextChannelById(r.getLong("replay")) == null) {
                    r.deleteRow()
                    x++
                }
            }
            x
        }
    }
}