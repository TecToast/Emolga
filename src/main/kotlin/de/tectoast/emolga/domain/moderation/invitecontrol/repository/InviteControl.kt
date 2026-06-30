package de.tectoast.emolga.domain.moderation.invitecontrol.repository

import de.tectoast.emolga.utils.cache.OneTimeCache
import de.tectoast.emolga.utils.suspendTransaction
import kotlinx.coroutines.flow.associate
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.koin.core.annotation.Single


@Single
class InviteControlRepository(private val db: R2dbcDatabase) {
    val guilds = OneTimeCache {
        suspendTransaction(db, InviteControlTable) {
            selectAll().associate { it[guild] to it[adminRoleId] }
        }
    }
}

object InviteControlTable : Table("invite_control") {
    val guild = long("guildid")
    val adminRoleId = long("admin_role_id")

    override val primaryKey = PrimaryKey(guild)
}