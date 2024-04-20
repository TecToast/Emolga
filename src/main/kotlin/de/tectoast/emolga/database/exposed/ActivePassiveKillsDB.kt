package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object ActivePassiveKillsDB : Table("activepassivekills") {
    val GUILD = long("guild")
    override val primaryKey = PrimaryKey(GUILD)

    suspend fun hasEnabled(guild: Long) = newSuspendedTransaction {
        selectAll().where { GUILD eq guild }.firstOrNull() != null
    }
}
