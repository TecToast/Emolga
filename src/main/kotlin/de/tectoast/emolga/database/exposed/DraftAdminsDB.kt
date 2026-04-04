package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single

@Single
class DraftAdminRepository(val db: R2dbcDatabase) {

    suspend fun isAdmin(guild: Long, userId: Long, roles: Collection<Long>) = suspendTransaction(db) {
        DraftAdminsTable.selectAll().where {
            (DraftAdminsTable.guild eq guild or (DraftAdminsTable.guild eq 0)) and ((DraftAdminsTable.roleid inList roles) or (DraftAdminsTable.userid eq userId))
        }.count() > 0
    }


}

object DraftAdminsTable : Table("draftadmins") {
    val guild = long("guildid")
    val roleid = long("roleid").nullable()
    val userid = long("userid").nullable()

    override val primaryKey = PrimaryKey(guild, roleid, userid)
}
