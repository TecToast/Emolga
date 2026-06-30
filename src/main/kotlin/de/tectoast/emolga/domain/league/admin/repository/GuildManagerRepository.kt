package de.tectoast.emolga.domain.league.admin.repository

import de.tectoast.emolga.utils.BotConstants
import de.tectoast.emolga.utils.suspendTransaction
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.insertIgnore
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.koin.core.annotation.Single

@Single
class GuildManagerRepository(private val db: R2dbcDatabase, private val botConstants: BotConstants) {


    suspend fun isAuthorized(user: Long, guild: Long): Boolean {
        return suspendTransaction(db, GuildManagerTable) {
            selectAll().where { (this.guild eq guild) and (this.user eq user) }.count() > 0
        }
    }


    /**
     * Checks whether a user is authorized for any guild
     * @param user the user id
     * @return *true* if the user is authorized for any guild, *false* otherwise
     */
    suspend fun isUserAuthorized(user: Long): Boolean {
        if (user == botConstants.botOwnerId) return true
        return suspendTransaction(db, GuildManagerTable) {
            selectAll().where { this.user eq user }.count() > 0
        }
    }

    suspend fun authorizeUser(guild: Long, user: Long) {
        suspendTransaction(db, GuildManagerTable) {
            insertIgnore {
                it[this.guild] = guild
                it[this.user] = user
            }
        }
    }

    suspend fun getDirectlyAuthorizedGuilds(user: Long): Set<Long> {
        return suspendTransaction(db, GuildManagerTable) {
            select(guild).where { this.user eq user }.map { it[guild] }.toSet()
        }
    }
}


object GuildManagerTable : Table("guildmanager") {
    val guild = long("guild")
    val user = long("user")
    override val primaryKey = PrimaryKey(guild, user)

}
