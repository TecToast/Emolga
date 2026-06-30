package de.tectoast.emolga.domain.pokemon.repository

import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.utils.database.showdownIDColumn
import de.tectoast.emolga.utils.referencesCascade
import de.tectoast.emolga.utils.suspendTransaction
import de.tectoast.emolga.utils.toShowdownID
import kotlinx.coroutines.flow.associate
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.batchInsert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single

@Single
class PokemonResolverRepository(
    private val db: R2dbcDatabase
) {

    suspend fun getAllDictionaryIds(): Set<ShowdownID> = suspendTransaction(db, PokemonDictionaryTable) {
        select(showdownId).map { it[showdownId] }.toSet()
    }

    suspend fun getAllDefaultAliasIds(): Map<ShowdownID, ShowdownID> = suspendTransaction(db, PokemonAliasTable) {
        select(aliasId, showdownId).where { guildId eq 0 }.associate { it[aliasId] to it[showdownId] }
    }

    suspend fun getById(search: ShowdownID): ShowdownID? = suspendTransaction(db, PokemonDictionaryTable) {
        select(showdownId).where { showdownId eq search }.firstOrNull()?.get(showdownId)
    }

    suspend fun getAllById(search: Iterable<ShowdownID>): Set<ShowdownID> =
        suspendTransaction(db, PokemonDictionaryTable) {
            select(showdownId).where { showdownId inList search }.map { it[showdownId] }.toSet()
        }

    suspend fun getByAlias(search: ShowdownID, guildId: Long): ShowdownID? = suspendTransaction(db) {
        PokemonAliasTable.join(
            PokemonDictionaryTable,
            JoinType.INNER,
            additionalConstraint = { PokemonAliasTable.showdownId eq PokemonDictionaryTable.showdownId })
            .select(PokemonDictionaryTable.showdownId).where {
                (PokemonAliasTable.aliasId eq search) and (PokemonAliasTable.guildId eq guildId or (PokemonAliasTable.guildId eq 0))
            }.orderBy(PokemonAliasTable.guildId to SortOrder.DESC).limit(1).firstOrNull()
            ?.get(PokemonDictionaryTable.showdownId)
    }

    suspend fun getAllValidAliases(guild: Long, aliases: Iterable<ShowdownID>) = suspendTransaction(
        db,
        PokemonAliasTable
    ) {
        select(aliasId, showdownId).where { (guildId eq guild or (guildId eq 0)) and (aliasId inList aliases) }
            .associate { it[aliasId] to it[showdownId] }
    }

    suspend fun addAliases(guild: Long, aliases: Map<String, ShowdownID>) =
        suspendTransaction(db, PokemonAliasTable) {
            batchInsert(aliases.entries, ignore = true, shouldReturnGeneratedValues = false) { (alias, target) ->
                this[PokemonAliasTable.aliasId] = alias.toShowdownID()
                this[PokemonAliasTable.showdownId] = target
                this[PokemonAliasTable.guildId] = guild
            }
        }

}

object PokemonAliasTable : Table("pokemon_aliases") {
    val aliasId = showdownIDColumn("alias_id")
    val showdownId = showdownIDColumn().referencesCascade(PokemonDictionaryTable.showdownId)
    val guildId = long("guild_id")

    override val primaryKey = PrimaryKey(aliasId, guildId)
}