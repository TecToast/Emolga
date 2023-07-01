package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object MutedRolesDB : Table("mutedroles") {
    val GUILD = long("guild")
    val ROLE = long("role")

    fun getMutedRole(guild: Long) = transaction {
        select { GUILD eq guild }.firstOrNull()?.get(ROLE)
    }
}
