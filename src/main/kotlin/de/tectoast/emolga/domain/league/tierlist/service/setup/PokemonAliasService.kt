package de.tectoast.emolga.domain.league.tierlist.service.setup

import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.domain.pokemon.repository.PokemonNamesRepository
import de.tectoast.emolga.domain.pokemon.repository.PokemonResolverRepository
import de.tectoast.emolga.utils.cache.TimedCache
import de.tectoast.emolga.utils.toShowdownID
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

@Single
class PokemonAliasService(
    private val pokemonNamesRepo: PokemonNamesRepository,
    private val pokemonResolverRepo: PokemonResolverRepository,
    clock: Clock
) {
    private val dictionaryCache = TimedCache(7.days, clock) {
        pokemonResolverRepo.getAllDictionaryIds()
    }
    private val defaultAliasCache = TimedCache(7.days, clock) {
        pokemonResolverRepo.getAllDefaultAliasIds()
    }

    suspend fun addExplicitAliases(guild: Long, explicitAliases: Map<String, ShowdownID>) {
        val cache = dictionaryCache()
        val allowedAliases = explicitAliases.filter { (_, official) -> official in cache }
        pokemonResolverRepo.addAliases(guild, allowedAliases)
        pokemonNamesRepo.addDisplayNames(guild, allowedAliases)
    }

    suspend fun addRegionalAliases(
        guild: Long,
        regionalForms: Map<String, String>,
        unresolvedPokemon: Iterable<String>
    ) {
        val regionalAliases = mutableMapOf<String, ShowdownID>()
        val regexes = regionalForms.mapKeys { (display, _) -> Regex(display.replace("***", "(.+)")) }
        val defaultAliases = defaultAliasCache()
        for (pokemon in unresolvedPokemon) {
            for ((displayRegex, official) in regexes) {
                displayRegex.matchEntire(pokemon)?.let { mr ->
                    val placeholderValue = mr.groupValues[1]
                    val officialName = official.replace("***", placeholderValue)
                    val officialId = officialName.toShowdownID()
                    regionalAliases[pokemon] = defaultAliases[officialId] ?: officialId
                }
            }
        }
        addExplicitAliases(guild, regionalAliases)
    }
}