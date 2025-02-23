package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object GuildManagerDB : Table("guildmanager") {
    val GUILD = long("guild")
    val USER = long("user")
    override val primaryKey = PrimaryKey(GUILD, USER)

    suspend fun getGuildsForUser(user: Long): List<Long> {
        return newSuspendedTransaction {
            select(GUILD).where { USER eq user }.map { it[GUILD] }
        }
    }

    suspend fun isUserAuthorized(user: Long): Boolean {
        return newSuspendedTransaction {
            select(USER).where { USER eq user }.count() > 0
        }
    }
}