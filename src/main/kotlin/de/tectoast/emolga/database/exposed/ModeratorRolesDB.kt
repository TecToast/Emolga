package de.tectoast.emolga.database.exposed

import net.dv8tion.jda.api.entities.Member
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object ModeratorRolesDB : Table("mutedroles") {
    val GUILD = long("guild")
    val ROLE = long("role")


    fun isModGuild(guild: Long) = transaction {
        select { GUILD eq guild }.count() > 0
    }

    fun isMod(m: Member) = transaction {
        select { ROLE inList m.roles.map { it.idLong } }.count() > 0
    }
}
