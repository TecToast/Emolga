package de.tectoast.emolga.domain.pokemon.service

import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.domain.pokemon.repository.PokedexRepository
import de.tectoast.emolga.domain.pokemon.repository.PokemonAlternativeFormesRepository
import de.tectoast.emolga.utils.toShowdownID
import org.koin.core.annotation.Single

@Single
class PokemonPossibleFormsResolver(
    private val pokedexRepo: PokedexRepository,
    private val alternativeFormesRepo: PokemonAlternativeFormesRepository
) {
    suspend fun getAllPossibleForms(guild: Long, ids: Iterable<ShowdownID>): Map<ShowdownID, Set<ShowdownID>> {
        val destination = mutableMapOf<ShowdownID, Set<ShowdownID>>()
        for (id in ids) {
            pokedexRepo.lookup(id)?.let { pokemon ->
                val formes = pokemon.otherFormes
                val baseSpecies = pokemon.baseSpecies
                destination[id] = buildSet {
                    add(id)
                    add(pokemon.name.toShowdownID())
                    if (formes != null) addAll(formes.map(String::toShowdownID))
                    if (baseSpecies != null) {
                        val baseId = baseSpecies.toShowdownID()
                        add(baseId)
                        val baseOtherForms = pokedexRepo.lookup(baseId)?.otherFormes
                        if (baseOtherForms != null) addAll(baseOtherForms.map(String::toShowdownID))
                    }
                    addAll(alternativeFormesRepo.getAlternativeFormes(guild, id))
                }
            }
        }
        return destination
    }
}