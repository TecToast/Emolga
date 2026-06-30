package de.tectoast.emolga.domain.league.tierlist.repository

import de.tectoast.emolga.domain.league.tierlist.model.TierlistMeta
import de.tectoast.emolga.domain.league.tierlist.model.config.TierlistConfig
import de.tectoast.emolga.domain.league.tierlist.repository.TierlistMetaTable.config
import de.tectoast.emolga.domain.league.transaction.model.TransactionPokemonData
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.utils.Language
import de.tectoast.emolga.utils.database.showdownIDColumn
import de.tectoast.emolga.utils.jsonb
import de.tectoast.emolga.utils.referencesCascade
import kotlinx.coroutines.flow.*
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single


@Single
class TierlistRepository(private val db: R2dbcDatabase) {

    private fun baseMetaPredicate(guild: Long, identifier: String) =
        (TierlistMetaTable.guild eq guild) and (TierlistMetaTable.identifier eq identifier)

    private suspend fun getTierlistId(guild: Long, identifier: String) = TierlistMetaTable.select(TierlistMetaTable.id)
        .where(baseMetaPredicate(guild, identifier))
        .firstOrNull()?.get(TierlistMetaTable.id)

    private val joinedTable = TierlistEntryTable.innerJoin(TierlistMetaTable, { this.tierlistId }, { this.id })

    suspend fun getMeta(guildId: Long, identifier: String): TierlistMeta? = suspendTransaction(db) {
        TierlistMetaTable.selectAll()
            .where { (TierlistMetaTable.guild eq guildId) and (TierlistMetaTable.identifier eq identifier) }
            .firstOrNull()?.let {
                TierlistMeta(
                    guild = it[TierlistMetaTable.guild],
                    identifier = it[TierlistMetaTable.identifier],
                    language = it[TierlistMetaTable.language],
                    config = it[config]
                )
            }
    }

    suspend fun getAllMetasForGuild(guildId: Long): List<TierlistMeta> = suspendTransaction(db) {
        TierlistMetaTable.selectAll()
            .where { TierlistMetaTable.guild eq guildId }
            .map {
                TierlistMeta(
                    guild = it[TierlistMetaTable.guild],
                    identifier = it[TierlistMetaTable.identifier],
                    language = it[TierlistMetaTable.language],
                    config = it[config]
                )
            }.toList()
    }

    suspend fun upsertMeta(meta: TierlistMeta) = suspendTransaction(db) {
        TierlistMetaTable.upsert(TierlistMetaTable.guild, TierlistMetaTable.identifier) {
            it[guild] = meta.guild
            it[identifier] = meta.identifier
            it[language] = meta.language
            it[config] = meta.config
        }
    }

    suspend fun setTierlistEntries(guildId: Long, identifier: String, entries: List<Pair<ShowdownID, String>>) =
        suspendTransaction(db) {
            val tierlistId = getTierlistId(guildId, identifier) ?: return@suspendTransaction false
            TierlistEntryTable.deleteWhere { TierlistEntryTable.tierlistId eq tierlistId }
            TierlistEntryTable.batchInsert(
                entries,
                ignore = true,
                shouldReturnGeneratedValues = false
            ) { (showdownId, tier) ->
                this[TierlistEntryTable.tierlistId] = tierlistId
                this[TierlistEntryTable.showdownId] = showdownId
                this[TierlistEntryTable.tier] = tier
            }
            true
        }

    suspend fun addOrUpdateEntry(guildId: Long, identifier: String, showdownId: ShowdownID, tier: String) =
        suspendTransaction(db) {
            val tierlistId = getTierlistId(guildId, identifier) ?: return@suspendTransaction false
            TierlistEntryTable.upsert {
                it[this.tierlistId] = tierlistId
                it[this.showdownId] = showdownId
                it[this.tier] = tier
            }
        }

    suspend fun getTier(guildId: Long, identifier: String, showdownId: ShowdownID): String? = suspendTransaction(db) {
        joinedTable.select(TierlistEntryTable.tier)
            .where {
                baseMetaPredicate(guildId, identifier) and
                        (TierlistEntryTable.showdownId eq showdownId)
            }
            .singleOrNull()?.get(TierlistEntryTable.tier)
    }

    suspend fun getAllShowdownIds(guild: Long, identifier: String): List<ShowdownID> = suspendTransaction(db) {
        joinedTable.select(TierlistEntryTable.showdownId)
            .where {
                (TierlistMetaTable.guild eq guild) and
                        (TierlistMetaTable.identifier eq identifier)
            }
            .map { it[TierlistEntryTable.showdownId] }.toList()
    }

    suspend fun getByTier(guild: Long, identifier: String, tier: String) = suspendTransaction(db) {
        joinedTable.select(TierlistEntryTable.showdownId)
            .where(baseMetaPredicate(guild, identifier) and (TierlistEntryTable.tier eq tier))
            .map { it[TierlistEntryTable.showdownId] }.toList()
    }

    suspend fun getByTierAndType(guild: Long, identifier: String, tier: String, type: String) = suspendTransaction(db) {
        joinedTable.select(TierlistEntryTable.showdownId).where(
            baseMetaPredicate(
                guild,
                identifier
            ) and (TierlistEntryTable.tier eq tier) and (TierlistEntryTable.type eq type)
        ).map { it[TierlistEntryTable.showdownId] }.toList()
    }

    suspend fun addPokemon(guild: Long, identifier: String, showdownId: ShowdownID, tier: String) =
        suspendTransaction(db) {
            val tierlistId = getTierlistId(guild, identifier) ?: return@suspendTransaction false
            TierlistEntryTable.insert {
                it[this.tierlistId] = tierlistId
                it[this.showdownId] = showdownId
                it[this.tier] = tier
            }
            true
        }

    suspend fun getAllPokemonWithTeraTier(guild: Long, identifier: String, teraIdentifier: String) =
        suspendTransaction(db) {
            val normalTL = joinedTable.select(TierlistEntryTable.showdownId, TierlistEntryTable.tier)
                .where(baseMetaPredicate(guild, identifier))
                .associate { it[TierlistEntryTable.showdownId] to it[TierlistEntryTable.tier] }
            val teraTL = joinedTable.select(TierlistEntryTable.showdownId, TierlistEntryTable.tier)
                .where(baseMetaPredicate(guild, teraIdentifier))
                .associate { it[TierlistEntryTable.showdownId] to it[TierlistEntryTable.tier] }
            normalTL.map { (mon, tier) ->
                val tera = teraTL[mon]
                TransactionPokemonData(mon, tier, tera)
            }
        }

}

object TierlistMetaTable : Table("tierlist_meta") {
    val id = long("tierlist_id").autoIncrement()
    val guild = long("guild_id")
    val identifier = text("identifier")
    val language = enumerationByName<Language>("language", 20)

    val config = jsonb<TierlistConfig>("config")

    override val primaryKey = PrimaryKey(id)

    init {
        index(true, guild, identifier)
    }
}

object TierlistEntryTable : Table("tierlist_entries") {
    val tierlistId = long("tierlist_id").referencesCascade(TierlistMetaTable.id)
    val showdownId = showdownIDColumn()
    val tier = text("tier")
    val type = text("type").nullable()

    override val primaryKey = PrimaryKey(tierlistId, showdownId)
}
