package de.tectoast.emolga.domain.dexquiz.service

import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.domain.pokemon.repository.PokedexRepository
import de.tectoast.emolga.domain.pokemon.service.PokemonDisplayService
import de.tectoast.emolga.utils.Language
import de.tectoast.emolga.utils.toShowdownID
import org.koin.core.annotation.Single

@Single
class DexEntryRedactService(
    private val pokedexRepo: PokedexRepository,
    private val pokemonDisplayService: PokemonDisplayService
) {
    suspend fun redactDexEntryText(showdownId: ShowdownID, text: String): String {
        val data = pokedexRepo.get(showdownId) ?: return text
        val baseFormId = data.baseSpecies?.toShowdownID()
        val toRedact =
            pokemonDisplayService.getDisplayNames(listOfNotNull(showdownId, baseFormId), 0, Language.GERMAN).values
        return toRedact.fold(text) { acc, name -> acc.replace(name, "[redacted]", ignoreCase = true) }
    }
}