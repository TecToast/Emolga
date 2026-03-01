package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.utils.createCoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.r2dbc.upsert

object GuildUsingReplayDB : Table("guildusingreplay") {
    val GUILDID = long("guildid")
    val NAME = varchar("name", 100)

    private val scope = createCoroutineScope("GuildUsingReplayDB")

    override val primaryKey = PrimaryKey(GUILDID)

    fun add(guildId: Long, name: String) = scope.launch {
        dbTransaction {
            upsert {
                it[GUILDID] = guildId
                it[NAME] = name
            }
        }
    }
}