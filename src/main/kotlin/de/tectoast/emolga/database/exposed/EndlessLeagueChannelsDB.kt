package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.sql.Table

object EndlessLeagueChannelsDB : Table("endlessleaguechannels") {
    val CHANNEL = long("channel")
    val ID = varchar("id", 7)

    override val primaryKey = PrimaryKey(CHANNEL)
}
