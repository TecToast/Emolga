package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.exposed.GuildManagerTable.GUILD
import de.tectoast.emolga.database.exposed.GuildManagerTable.USER
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.mdb
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.insertIgnore
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single

@Single
class GuildManagerRepository(val db: R2dbcDatabase) {

    /**
     * Gets all guilds the user is authorized for
     * @return the list containing the guild ids
     */
    suspend fun getGuildsForUser(user: Long): Set<Long> {
        val result = suspendTransaction(db) {
            GuildManagerTable.select(GUILD).where { USER eq user }.map { it[GUILD] }.toSet()
        }
        if (user == Constants.FLOID) {
            return result + mdb.league.find().toFlow().map { it.guild }.toSet() + Constants.G.MY
        }
        return result
    }


    /**
     * Checks whether a user is authorized for any guild
     * @param user the user id
     * @return *true* if the user is authorized for any guild, *false* otherwise
     */
    suspend fun isUserAuthorized(user: Long): Boolean {
        if (user == Constants.FLOID) return true
        return suspendTransaction(db) {
            GuildManagerTable.select(USER).where { USER eq user }.count() > 0
        }
    }

    suspend fun authorizeUser(guild: Long, user: Long) {
        suspendTransaction(db) {
            GuildManagerTable.insertIgnore {
                it[GUILD] = guild
                it[USER] = user
            }
        }
    }
}

object GuildManagerTable : Table("guildmanager") {
    val GUILD = long("guild")
    val USER = long("user")
    override val primaryKey = PrimaryKey(GUILD, USER)

}
