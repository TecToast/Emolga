package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.v1.core.Table
import org.koin.core.annotation.Single

@Single
class PredictionGameVotesDB : Table("predictiongamevotes") {
    val leaguename = varchar("leaguename", 31)
    val userid = long("userid")
    val week = integer("gameday")
    val battle = integer("battle")
    val idx = integer("idx")
    val correct = bool("correct").nullable().default(null)

    override val primaryKey = PrimaryKey(leaguename, userid, week, battle)
}
