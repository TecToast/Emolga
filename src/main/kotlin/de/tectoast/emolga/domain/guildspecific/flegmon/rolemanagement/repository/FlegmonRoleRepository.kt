package de.tectoast.emolga.domain.guildspecific.flegmon.rolemanagement.repository

import de.tectoast.emolga.domain.guildspecific.flegmon.rolemanagement.model.RoleData
import de.tectoast.emolga.utils.suspendTransaction
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.koin.core.annotation.Single

@Single
class FlegmonRoleRepository(private val db: R2dbcDatabase) {
    suspend fun getRoles(): List<RoleData> = suspendTransaction(db, RoleDataTable) {
        selectAll().map { row ->
            RoleData(
                compId = row[compId],
                name = row[name],
                description = row[description],
                roleId = row[roleId],
                formattedEmoji = row[formattedEmoji]
            )
        }.toList()
    }
}


object RoleDataTable : Table("flegmon_role_data") {
    val compId = text("comp_id")
    val name = text("name")
    val description = text("description")
    val roleId = long("role_id")
    val formattedEmoji = text("formatted_emoji").nullable()
}