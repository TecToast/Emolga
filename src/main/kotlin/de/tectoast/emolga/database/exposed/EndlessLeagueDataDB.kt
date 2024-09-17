package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.sql.Table

object EndlessLeagueDataDB : Table("endlessleaguedata") {
    val ID = varchar("id", 7)
    val URL = varchar("url", 127)

    override val primaryKey = PrimaryKey(ID, URL)
}
