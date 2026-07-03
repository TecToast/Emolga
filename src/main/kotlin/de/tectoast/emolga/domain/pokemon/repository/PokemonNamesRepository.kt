package de.tectoast.emolga.domain.pokemon.repository

import de.tectoast.emolga.domain.pokemon.model.PokemonNameRawData
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.domain.pokemon.model.showdownIDColumn
import de.tectoast.emolga.utils.referencesCascade
import de.tectoast.emolga.utils.suspendTransaction
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.transform
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.batchInsert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single

@Single
class PokemonNamesRepository(private val db: R2dbcDatabase) {

    suspend fun getRawNames(showdownIds: Iterable<ShowdownID>, guild: Long): List<PokemonNameRawData> {
        return suspendTransaction(db) {
            PokemonDictionaryTable.join(
                PokemonDisplayNameTable, JoinType.LEFT, additionalConstraint = {
                    (PokemonDisplayNameTable.showdownId eq PokemonDictionaryTable.showdownId) and (PokemonDisplayNameTable.guildId eq guild)
                }).selectAll().where { PokemonDictionaryTable.showdownId inList showdownIds }.map { row ->
                PokemonNameRawData(
                    showdownId = row[PokemonDictionaryTable.showdownId],
                    nameEn = row[PokemonDictionaryTable.nameEn],
                    nameDe = row[PokemonDictionaryTable.nameDe],
                    customGuildName = row.getOrNull(PokemonDisplayNameTable.customDisplayName)
                )
            }.toList()
        }
    }

    suspend fun getOfficialNames(query: String, limit: Int) = suspendTransaction(db) {
        PokemonDictionaryTable.selectAll()
            .where { PokemonDictionaryTable.nameEn like "$query%" or (PokemonDictionaryTable.nameDe like "$query%") }
            .limit(limit)
            .transform {
                emit(it[PokemonDictionaryTable.nameEn])
                emit(it[PokemonDictionaryTable.nameDe])
            }.take(limit).toList()
    }

    suspend fun addDisplayNames(guild: Long, names: Map<String, ShowdownID>) = suspendTransaction(
        db,
        PokemonDisplayNameTable
    ) {
        batchInsert(names.entries, ignore = true, shouldReturnGeneratedValues = false) { (displayName, showdownId) ->
            this[PokemonDisplayNameTable.guildId] = guild
            this[PokemonDisplayNameTable.showdownId] = showdownId
            this[PokemonDisplayNameTable.customDisplayName] = displayName
        }
    }
}


object PokemonDictionaryTable : Table("pokemon_dictionary") {
    val showdownId = showdownIDColumn().referencesPokedex()
    val nameEn = text("name_en")
    val nameDe = text("name_de")

    override val primaryKey = PrimaryKey(showdownId)
}

object PokemonDisplayNameTable : Table("pokemon_display_names") {
    val guildId = long("guild_id")
    val showdownId = showdownIDColumn().referencesCascade(PokemonDictionaryTable.showdownId)
    val customDisplayName = text("custom_display_name")

    override val primaryKey = PrimaryKey(guildId, showdownId)
}
