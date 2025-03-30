package de.tectoast.emolga.database.exposed

import net.dv8tion.jda.api.entities.Member
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object DraftAdminsDB : Table("draftadmins") {
    val GUILD = long("guildid")
    private val ROLEID = long("roleid").nullable()
    val USERID = long("userid").nullable()

    /**
     * Checks whether a user is a draft admin in a guild
     * @param gid the guild id
     * @param mem the member to check
     * @return *true* if the user is a draft admin, *false* otherwise
     */
    suspend fun isAdmin(gid: Long, mem: Member) = newSuspendedTransaction {
        selectAll().where {
            (GUILD eq gid) and ((ROLEID inList mem.roles.map { it.idLong }) or (USERID eq mem.idLong))
        }.count() > 0
    }
}
