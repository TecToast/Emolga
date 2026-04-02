package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.v1.core.Table
import org.koin.core.annotation.Single

@Single
class TeamGraphicMessageDB : Table("teamgraphicmessage") {
    val LEAGUE = varchar("league", 100)
    val IDX = integer("idx")
    val MESSAGEID = long("messageid")

    override val primaryKey = PrimaryKey(LEAGUE, IDX)
}
