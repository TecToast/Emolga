package de.tectoast.emolga.domain.pokemon.service

import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.domain.pokemon.repository.PokedexRepository
import de.tectoast.emolga.domain.pokemon.repository.PokemonNamesRepository
import de.tectoast.emolga.utils.Language
import de.tectoast.emolga.utils.toShowdownID
import org.koin.core.annotation.Single

@Single
class PokemonDisplayService(
    private val repository: PokemonNamesRepository,
    private val pokedexRepo: PokedexRepository
) {
    suspend fun getDisplayNames(
        showdownIds: Iterable<ShowdownID>,
        guildId: Long,
        language: Language
    ): Map<ShowdownID, String> {
        val rawDataList = repository.getRawNames(showdownIds, guildId)
        val result = mutableMapOf<ShowdownID, String>()
        for (data in rawDataList) {
            val officialName = if (language == Language.GERMAN) data.nameDe else data.nameEn
            result[data.showdownId] = data.customGuildName ?: officialName
        }
        return result
    }

    suspend fun getAutocompleteNames(showdownIds: Iterable<ShowdownID>, guild: Long): Map<ShowdownID, List<String>> {
        val rawDataList = repository.getRawNames(showdownIds, guild)
        val result = mutableMapOf<ShowdownID, List<String>>()
        for (data in rawDataList) {
            result[data.showdownId] = listOfNotNull(data.customGuildName, data.nameEn, data.nameDe)
        }
        return result
    }

    suspend fun getDisplayName(showdownId: ShowdownID, guildId: Long, language: Language): String {
        return getDisplayNames(listOf(showdownId), guildId, language)[showdownId] ?: showdownId.value
    }

    suspend fun getDisplayNamesOfReplay(
        showdownIds: Iterable<ShowdownID>,
        guildId: Long,
        language: Language
    ): Map<ShowdownID, String?> {
        val pokedexLookup = pokedexRepo.getAll(showdownIds).mapValues { (_, mon) ->
            (if (mon.requiredAbility != null) mon.baseSpeciesOrName else mon.name).toShowdownID()
        }
        val lookupIds = showdownIds.map { pokedexLookup[it] ?: it }
        val displayNameLookup = getDisplayNames(lookupIds, guildId, language)
        return showdownIds.associateWith { id ->
            if (id.value.isBlank()) return@associateWith "_???_"
            val lookupId = pokedexLookup[id] ?: id
            displayNameLookup[lookupId]
        }
    }
}