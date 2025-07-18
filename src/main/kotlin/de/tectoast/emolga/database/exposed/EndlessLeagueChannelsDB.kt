package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.v1.core.Table


object EndlessLeagueChannelsDB : Table("endlessleaguechannels") {
    val CHANNEL = long("channel")
    val ID = varchar("id", 7)

    override val primaryKey = PrimaryKey(CHANNEL)
}
