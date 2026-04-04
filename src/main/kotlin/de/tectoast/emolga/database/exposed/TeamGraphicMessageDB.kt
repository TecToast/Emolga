package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.v1.core.Table

object TeamGraphicMessageTable : Table("teamgraphicmessage") {
    val league = varchar("league", 100)
    val idx = integer("idx")
    val messageId = long("messageid")

    override val primaryKey = PrimaryKey(league, idx)
}
