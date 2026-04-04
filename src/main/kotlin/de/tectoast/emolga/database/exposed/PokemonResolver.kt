package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.utils.Language
import de.tectoast.emolga.utils.toSDName
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single

data class ResolvedPokemon(val showdownId: String, val nameEn: String, val nameDe: String)

@Single
class PokemonResolverService(val repository: PokemonResolverRepository) {
    suspend fun resolvePokemon(input: String, guildId: Long): ResolvedPokemon? {
        val searchId = input.toSDName()
        return repository.getById(searchId) ?: repository.getByAlias(searchId, guildId)
    }
}

@Single
class PokemonOutputService(val repository: PokemonNamesRepository) {
    suspend fun getDisplayNames(
        showdownIds: List<String>,
        guildId: Long,
        language: Language
    ): Map<String, String> {
        val rawDataList = repository.getRawNames(showdownIds, guildId)
        val result = mutableMapOf<String, String>()
        for (data in rawDataList) {
            val officialName = if (language == Language.GERMAN) data.nameDe else data.nameEn
            result[data.showdownId] = data.customGuildName ?: officialName
        }
        return result
    }

    suspend fun getDisplayName(showdownId: String, guildId: Long, language: Language): String? {
        return getDisplayNames(listOf(showdownId), guildId, language)[showdownId]
    }
}

data class PokemonNameRawData(
    val showdownId: String, val nameEn: String, val nameDe: String, val customGuildName: String?
)

@Single
class PokemonResolverRepository(
    private val db: R2dbcDatabase,
    private val dictionary: PokemonDictionaryTable,
    private val aliases: PokemonAliasTable
) {
    private fun ResultRow.toResolvedPokemon() =
        ResolvedPokemon(this[dictionary.showdownId], this[dictionary.nameEn], this[dictionary.nameDe])

    suspend fun getById(searchId: String): ResolvedPokemon? = suspendTransaction(db) {
        dictionary.selectAll().where { dictionary.showdownId eq searchId }.firstOrNull()?.toResolvedPokemon()
    }

    suspend fun getByAlias(searchId: String, guildId: Long): ResolvedPokemon? = suspendTransaction(db) {
        aliases.join(dictionary, JoinType.INNER, additionalConstraint = { aliases.showdownId eq dictionary.showdownId })
            .selectAll().where {
                (aliases.aliasId eq searchId) and (aliases.guildId eq guildId or aliases.guildId.isNull())
            }.orderBy(aliases.guildId to SortOrder.DESC_NULLS_LAST).limit(1).firstOrNull()?.toResolvedPokemon()
    }

}

@Single
class PokemonNamesRepository(val db: R2dbcDatabase) {

    suspend fun getRawNames(showdownIds: List<String>, guild: Long): List<PokemonNameRawData> {
        return suspendTransaction(db) {

            PokemonDictionaryTable.join(
                PokemonDisplayNameTable, JoinType.LEFT, additionalConstraint = {
                    (PokemonDisplayNameTable.showdownId eq PokemonDictionaryTable.showdownId) and (PokemonDisplayNameTable.guildId eq guild)
                }).selectAll().where { PokemonDictionaryTable.showdownId inList showdownIds }.map { row ->
                PokemonNameRawData(
                    showdownId = row[PokemonDictionaryTable.showdownId],
                    nameEn = row[PokemonDictionaryTable.nameEn],
                    nameDe = row[PokemonDictionaryTable.nameDe],
                    customGuildName = row[PokemonDisplayNameTable.customDisplayName]
                )
            }.toList()
        }
    }
}


object PokemonDictionaryTable : Table("pokemon_dictionary") {
    val showdownId = varchar("showdownid", 100)
    val nameEn = varchar("nameen", 100)
    val nameDe = varchar("namede", 100)

    override val primaryKey = PrimaryKey(showdownId)
}


object PokemonAliasTable : Table("pokemon_aliases") {
    val aliasId = varchar("alias_id", 100)
    val showdownId = varchar("showdown_id", 100).references(PokemonDictionaryTable.showdownId)
    val guildId = long("guild_id").nullable()

    override val primaryKey = PrimaryKey(aliasId, guildId)
}

object PokemonDisplayNameTable : Table("pokemon_display_names") {
    val guildId = long("guild_id")
    val showdownId = varchar("showdown_id", 100).references(PokemonDictionaryTable.showdownId)
    val customDisplayName = varchar("custom_display_name", 100)

    override val primaryKey = PrimaryKey(guildId, showdownId)
}