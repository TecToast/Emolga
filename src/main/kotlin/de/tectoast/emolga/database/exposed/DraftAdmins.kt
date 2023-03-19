package de.tectoast.emolga.database.exposed

import net.dv8tion.jda.api.entities.Member
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object DraftAdmins : Table("draftadmins") {
    val guild = long("guildid")
    private val roleid = long("roleid").nullable()
    val userid = long("userid").nullable()

    fun isAdmin(gid: Long, mem: Member) = transaction {
        select {
            (guild eq gid) and ((roleid inList mem.roles.map { it.idLong }) or (userid eq mem.idLong))
        }.count() > 0
    }
}
