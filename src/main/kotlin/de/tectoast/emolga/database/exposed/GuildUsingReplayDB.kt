package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.utils.createCoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Single

interface GuildUsingReplayRepository {
    fun add(guild: Long, name: String)
}

@Single
class PostgresGuildUsingReplayRepository(val db: R2dbcDatabase, val guildUsingReplay: GuildUsingReplayDB) :
    GuildUsingReplayRepository {
    private val scope = createCoroutineScope("GuildUsingReplayDB")
    override fun add(guild: Long, name: String) {
        scope.launch {
            suspendTransaction(db) {
                guildUsingReplay.upsert {
                    it[guildUsingReplay.guildid] = guild
                    it[guildUsingReplay.name] = name
                }
            }
        }
    }
}

@Single
class GuildUsingReplayDB : Table("guildusingreplay") {
    val guildid = long("guildid")
    val name = varchar("name", 100)

    override val primaryKey = PrimaryKey(guildid)
}
