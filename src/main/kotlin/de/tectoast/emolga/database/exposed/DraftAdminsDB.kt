package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single

interface DraftAdminRepository {
    /**
     * Checks whether a user is a draft admin in a guild
     * @param guild the guild id
     * @param userId the userid of the user
     * @param roles the collection of roles of the user
     * @return *true* if the user is a draft admin, *false* otherwise
     */
    suspend fun isAdmin(guild: Long, userId: Long, roles: Collection<Long>): Boolean
}

@Single
class PostgresDraftAdminRepository(val db: R2dbcDatabase, val draftAdmin: DraftAdminsDB) : DraftAdminRepository {

    override suspend fun isAdmin(guild: Long, userId: Long, roles: Collection<Long>) = suspendTransaction(db) {
        draftAdmin.selectAll().where {
            (draftAdmin.guild eq guild or (draftAdmin.guild eq 0)) and ((draftAdmin.roleid inList roles) or (draftAdmin.userid eq userId))
        }.count() > 0
    }


}

@Single
class DraftAdminsDB : Table("draftadmins") {
    val guild = long("guildid")
    val roleid = long("roleid").nullable()
    val userid = long("userid").nullable()

    override val primaryKey = PrimaryKey(guild, roleid, userid)
}
