package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.sql.Table

object EndlessLeagueInfoDB : Table("endlessleagueinfo") {
    val ID = varchar("id", 7)
    val SID = varchar("sid", 63)
    val STARTCOORD = varchar("startcoord", 7)

    override val primaryKey = PrimaryKey(ID)
}
