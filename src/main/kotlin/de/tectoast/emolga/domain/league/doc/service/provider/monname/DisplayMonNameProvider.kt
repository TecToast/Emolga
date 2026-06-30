package de.tectoast.emolga.domain.league.doc.service.provider.monname

import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.domain.pokemon.service.PokemonDisplayService
import de.tectoast.emolga.utils.Language

class DisplayMonNameProvider(
    private val displayService: PokemonDisplayService,
    private val guildId: Long,
    private val language: Language
) :
    MonNameProvider {
    override suspend fun getDisplayName(showdownId: ShowdownID) =
        displayService.getDisplayName(showdownId, guildId, language)

}
