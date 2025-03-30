package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object ActivePassiveKillsDB : Table("activepassivekills") {
    val GUILD = long("guild")
    override val primaryKey = PrimaryKey(GUILD)

    /**
     * Returns whether the provided guild wants a separation between active and passive kills
     * @param guild the guild
     * @return true if the guild wants active/passive kills, false otherwise
     */
    suspend fun hasEnabled(guild: Long) = newSuspendedTransaction {
        selectAll().where { GUILD eq guild }.firstOrNull() != null
    }
}
