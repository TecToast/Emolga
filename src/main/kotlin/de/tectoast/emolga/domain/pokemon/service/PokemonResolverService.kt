package de.tectoast.emolga.domain.pokemon.service

import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.domain.pokemon.repository.PokemonResolverRepository
import de.tectoast.emolga.utils.toShowdownID
import org.koin.core.annotation.Single

@Single
class PokemonResolverService(private val repository: PokemonResolverRepository) {
    suspend fun resolvePokemon(input: String, guildId: Long): ShowdownID? {
        val search = input.toShowdownID()
        return repository.getById(search) ?: repository.getByAlias(search, guildId)
    }

    suspend fun getUnresolvedPokemon(guild: Long, pokemon: Iterable<String>): Set<String> {
        val searchIds = pokemon.associateBy { it.toShowdownID() }
        val resolvedIds = repository.getAllById(searchIds.keys)
        val validAliases = repository.getAllValidAliases(guild, searchIds.keys)
        return (searchIds - (resolvedIds + validAliases.keys)).values.toSet()
    }

    suspend fun resolveAllPokemon(guild: Long, pokemon: Iterable<String>): Map<String, ShowdownID> {
        val searchIds = pokemon.associateBy { it.toShowdownID() }
        val resolvedIds = repository.getAllById(searchIds.keys)
        val validAliases = repository.getAllValidAliases(guild, searchIds.keys)
        return searchIds.entries.mapNotNull { (searchId, original) ->
            val id = if (searchId in resolvedIds) {
                searchId
            } else {
                validAliases[searchId]
            }
            if (id == null) null else original to id
        }.toMap()
    }

    suspend fun resolveNested(guild: Long, nestedPokemon: Iterable<Iterable<String>>): List<List<ShowdownID>> {
        val allPokemon = nestedPokemon.flatten()
        val allResolved = resolveAllPokemon(guild, allPokemon)
        return nestedPokemon.map { it.mapNotNull { pokemon -> allResolved[pokemon] } }
    }
}