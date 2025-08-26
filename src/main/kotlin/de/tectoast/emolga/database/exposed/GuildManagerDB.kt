package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.select

object GuildManagerDB : Table("guildmanager") {
    val GUILD = long("guild")
    val USER = long("user")
    override val primaryKey = PrimaryKey(GUILD, USER)

    /**
     * Gets all guilds the user is authorized for
     * @return the list containing the guild ids
     */
    suspend fun getGuildsForUser(user: Long): Set<Long> {
        return dbTransaction {
            select(GUILD).where { USER eq user }.map { it[GUILD] }.toSet()
        }
    }

    /**
     * Checks whether a user is authorized for any guild
     * @param user the user id
     * @return *true* if the user is authorized for any guild, *false* otherwise
     */
    suspend fun isUserAuthorized(user: Long): Boolean {
        return dbTransaction {
            select(USER).where { USER eq user }.count() > 0
        }
    }
}
