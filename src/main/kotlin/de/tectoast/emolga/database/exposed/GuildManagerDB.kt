package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.mdb
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.insertIgnore
import org.jetbrains.exposed.v1.r2dbc.select

object GuildManagerDB : Table("guildmanager") {
    val GUILD = long("guild")
    val USER = long("user")
    override val primaryKey = PrimaryKey(GUILD, USER)

    /**
     * Gets all guilds the user is authorized for
     * @return the list containing the guild ids
     */
    suspend fun getGuildsForUser(user: Long): Set<Long> {
        val result = dbTransaction {
            select(GUILD).where { USER eq user }.map { it[GUILD] }.toSet()
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
        return dbTransaction {
            select(USER).where { USER eq user }.count() > 0
        }
    }

    suspend fun authorizeUser(guild: Long, user: Long) {
        dbTransaction {
            insertIgnore {
                it[GUILD] = guild
                it[USER] = user
            }
        }
    }
}
