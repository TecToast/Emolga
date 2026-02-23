package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.update
import org.jetbrains.exposed.v1.r2dbc.upsert

object PredictionGameVotesDB : Table("predictiongamevotes") {
    val LEAGUENAME = varchar("leaguename", 31)
    val USERID = long("userid")
    val GAMEDAY = integer("gameday")
    val BATTLE = integer("battle")
    val IDX = integer("idx")
    val CORRECT = bool("correct").nullable().default(null)

    override val primaryKey = PrimaryKey(LEAGUENAME, USERID, GAMEDAY, BATTLE)

    suspend fun updateCorrectBattles(league: String, gameday: Int, battle: Int, winningIndex: Int) = dbTransaction {
        update({ (LEAGUENAME eq league) and (GAMEDAY eq gameday) and (BATTLE eq battle) }) {
            it[CORRECT] = IDX eq winningIndex
        }
    }

    suspend fun addVote(user: Long, league: String, gameday: Int, battle: Int, idx: Int) = dbTransaction {
        upsert {
            it[USERID] = user
            it[LEAGUENAME] = league
            it[GAMEDAY] = gameday
            it[BATTLE] = battle
            it[IDX] = idx
        }
    }

    suspend fun getCurrentState(league: String, gameday: Int, battle: Int) = dbTransaction {
        val count = USERID.count()
        select(IDX, count).where { (LEAGUENAME eq league) and (GAMEDAY eq gameday) and (BATTLE eq battle) }
            .groupBy(IDX).toMap { it[IDX] to it[count] }
    }
}
