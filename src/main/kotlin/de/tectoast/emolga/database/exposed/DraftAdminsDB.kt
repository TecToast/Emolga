package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import net.dv8tion.jda.api.entities.Member
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.selectAll

object DraftAdminsDB : Table("draftadmins") {
    val GUILD = long("guildid")
    val ROLEID = long("roleid").nullable()
    val USERID = long("userid").nullable()

    /**
     * Checks whether a user is a draft admin in a guild
     * @param gid the guild id
     * @param mem the member to check
     * @return *true* if the user is a draft admin, *false* otherwise
     */
    suspend fun isAdmin(gid: Long, mem: Member) = dbTransaction {
        selectAll().where {
            (GUILD eq gid or (GUILD eq 0)) and ((ROLEID inList mem.roles.map { it.idLong }) or (USERID eq mem.idLong))
        }.count() > 0
    }
}
