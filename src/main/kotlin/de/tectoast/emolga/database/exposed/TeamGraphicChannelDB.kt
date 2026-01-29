package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.upsert

object TeamGraphicChannelDB : Table("teamgraphicchannel") {
    val LEAGUE = varchar("league", 100)
    val CHANNEL = long("channelid")

    override val primaryKey = PrimaryKey(LEAGUE)

    suspend fun set(league: String, channelId: Long) = dbTransaction {
        upsert {
            it[LEAGUE] = league
            it[CHANNEL] = channelId
        }
    }

    suspend fun getChannelId(league: String): Long? = dbTransaction {
        select(CHANNEL).where { LEAGUE eq league }.firstOrNull()?.get(CHANNEL)
    }
}