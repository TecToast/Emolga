package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.Database
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object TipGamesDB : Table("tipgame") {
    val USERID = long("userid")
    val LEAGUE_NAME = varchar("league_name", 20)
    val CORRECT_GUESSES = integer("correct_guesses")

    fun addPointToUser(user: Long, league: String) {
        Database.dbScope.launch {
            newSuspendedTransaction {
                val userobj = select { USERID eq user and (LEAGUE_NAME eq league) }.firstOrNull()
                if (userobj == null) {
                    insert {
                        it[USERID] = user
                        it[LEAGUE_NAME] = league
                        it[CORRECT_GUESSES] = 1
                    }
                } else {
                    update({ USERID eq user and (LEAGUE_NAME eq LEAGUE_NAME) }) {
                        it[CORRECT_GUESSES] = userobj[CORRECT_GUESSES] + 1
                    }
                }
            }
        }
    }
}

