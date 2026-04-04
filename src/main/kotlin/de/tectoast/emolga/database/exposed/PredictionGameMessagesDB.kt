package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.v1.core.Table

object PredictionGameMessagesTable : Table("predictiongamemessages") {
    val leaguename = varchar("leaguename", 31)
    val week = integer("gameday")
    val battle = integer("battle")
    val messageid = long("messageid")

    override val primaryKey = PrimaryKey(leaguename, week, battle)
}
