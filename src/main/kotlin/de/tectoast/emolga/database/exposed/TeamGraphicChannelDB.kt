package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.v1.core.Table

object TeamGraphicChannelTable : Table("teamgraphicchannel") {
    val league = varchar("league", 100)
    val channel = long("channelid")

    override val primaryKey = PrimaryKey(league)
}
