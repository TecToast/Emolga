package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.Database
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object TipGames : Table("tipgame") {
    val userid = long("userid")
    val leagueName = varchar("league_name", 20)
    val correctGuesses = integer("correct_guesses")

    fun addPointToUser(user: Long, league: String) {
        Database.dbScope.launch {
            newSuspendedTransaction {
                val userobj = select { userid eq user and (leagueName eq league) }.firstOrNull()
                if (userobj == null) {
                    insert {
                        it[userid] = user
                        it[leagueName] = league
                        it[correctGuesses] = 1
                    }
                } else {
                    update({ userid eq user and (leagueName eq leagueName) }) {
                        it[correctGuesses] = userobj[correctGuesses] + 1
                    }
                }
            }
        }
    }
}

