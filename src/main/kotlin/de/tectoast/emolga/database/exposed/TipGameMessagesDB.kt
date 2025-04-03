package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.upsert

object TipGameMessagesDB : Table("tipgamemessages") {
    val LEAGUENAME = varchar("leaguename", 31)
    val GAMEDAY = integer("gameday")
    val BATTLE = integer("battle")
    val MESSAGEID = long("messageid")

    /**
     * Gets the message id of the tipgame message of the league/gameday/battle
     * @param leagueName the league name
     * @param gameday the gameday
     * @param battle the battle index (if the messages are split)
     * @return a list containing all message ids corresponding to the specified criteria
     */
    suspend fun get(leagueName: String, gameday: Int, battle: Int? = null) = dbTransaction {
        select(MESSAGEID).where {
            val op = (LEAGUENAME eq leagueName) and (GAMEDAY eq gameday)
            if (battle == null) op
            else op and (BATTLE eq battle)
        }.map { it[MESSAGEID] }
    }

    /**
     * Sets the message id of a tipgame messsage given the league/gameday/battle
     * @param leagueName the league name
     * @param gameday the gameday
     * @param battle the battle index
     */
    suspend fun set(leagueName: String, gameday: Int, battle: Int, messageId: Long) = dbTransaction {
        upsert {
            it[LEAGUENAME] = leagueName
            it[GAMEDAY] = gameday
            it[BATTLE] = battle
            it[MESSAGEID] = messageId
        }
    }

}
