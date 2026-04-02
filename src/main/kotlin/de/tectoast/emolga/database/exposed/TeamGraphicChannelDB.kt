package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.v1.core.Table
import org.koin.core.annotation.Single

@Single
class TeamGraphicChannelDB : Table("teamgraphicchannel") {
    val LEAGUE = varchar("league", 100)
    val CHANNEL = long("channelid")

    override val primaryKey = PrimaryKey(LEAGUE)
}
