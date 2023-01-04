package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object ActivePassiveKills : Table("activepassivekills") {
    val guild = long("guild")
    override val primaryKey = PrimaryKey(guild)

    suspend fun hasEnabled(guild: Long) = newSuspendedTransaction {
        select { ActivePassiveKills.guild eq guild }.firstOrNull() != null
    }
}
