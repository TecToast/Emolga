package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.exposed.TierlistMetaTable.priceManager
import de.tectoast.emolga.utils.Language
import de.tectoast.emolga.utils.draft.TierlistPriceManager
import de.tectoast.emolga.utils.jsonb
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.singleOrNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Single

data class TierlistMeta(
    val guild: Long,
    val identifier: String,
    val language: Language,
    val priceManager: TierlistPriceManager
) {
    // TODO: Support multiple prices
    val order get() = priceManager.getTiers()

    inline fun <reified T : TierlistPriceManager> has() = priceManager is T
}

data class TierlistEntry(
    val showdownId: String,
    val tier: String,
    val type: String? = null
)

internal object TierlistMetaTable : Table("tierlist_meta") {
    val guild = long("guild_id")
    val identifier = varchar("identifier", 30)
    val language = enumerationByName<Language>("language", 20)

    val priceManager = jsonb<TierlistPriceManager>("price_manager")

    override val primaryKey = PrimaryKey(guild, identifier)
}

internal object TierlistEntryTable : Table("tierlist_entries") {
    val guildId = long("guild_id")
    val identifier = varchar("identifier", 30)
    val showdownId = varchar("showdown_id", 50)
    val tier = varchar("tier", 8)
    val type = varchar("type", 10).nullable()

    override val primaryKey = PrimaryKey(guildId, identifier, showdownId)
}

@Single
class TierlistRepository(private val db: R2dbcDatabase) {


    suspend fun getMeta(guildId: Long, identifier: String): TierlistMeta? = suspendTransaction(db) {
        TierlistMetaTable.selectAll()
            .where { (TierlistMetaTable.guild eq guildId) and (TierlistMetaTable.identifier eq identifier) }
            .firstOrNull()?.let {
                TierlistMeta(
                    guild = it[TierlistMetaTable.guild],
                    identifier = it[TierlistMetaTable.identifier],
                    language = it[TierlistMetaTable.language],
                    priceManager = it[priceManager]
                )
            }
    }

    suspend fun upsertMeta(meta: TierlistMeta) = suspendTransaction(db) {
        TierlistMetaTable.upsert {
            it[guild] = meta.guild
            it[identifier] = meta.identifier
            it[language] = meta.language
            it[priceManager] = meta.priceManager
        }
    }

    suspend fun addOrUpdateEntry(guildId: Long, identifier: String, showdownId: String, tier: String) =
        suspendTransaction(db) {
            TierlistEntryTable.upsert {
                it[this.guildId] = guildId
                it[this.identifier] = identifier
                it[this.showdownId] = showdownId
                it[this.tier] = tier
            }
        }

    suspend fun getTier(guildId: Long, identifier: String, showdownId: String): String? = suspendTransaction(db) {
        TierlistEntryTable.select(TierlistEntryTable.tier)
            .where {
                (TierlistEntryTable.guildId eq guildId) and
                        (TierlistEntryTable.identifier eq identifier) and
                        (TierlistEntryTable.showdownId eq showdownId)
            }
            .singleOrNull()?.get(TierlistEntryTable.tier)
    }

}