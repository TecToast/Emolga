package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.utils.createCoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Single

@Single
class GuildUsingReplayRepository(val db: R2dbcDatabase) {
    private val scope = createCoroutineScope("GuildUsingReplayTable")

    fun add(guild: Long, name: String) {
        scope.launch {
            suspendTransaction(db) {
                GuildUsingReplayTable.upsert {
                    it[GuildUsingReplayTable.guildid] = guild
                    it[GuildUsingReplayTable.name] = name
                }
            }
        }
    }
}

object GuildUsingReplayTable : Table("guildusingreplay") {
    val guildid = long("guildid")
    val name = varchar("name", 100)

    override val primaryKey = PrimaryKey(guildid)
}
