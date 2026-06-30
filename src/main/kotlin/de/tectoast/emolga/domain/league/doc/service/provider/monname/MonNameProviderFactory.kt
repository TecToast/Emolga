package de.tectoast.emolga.domain.league.doc.service.provider.monname

import de.tectoast.emolga.domain.pokemon.service.PokemonDisplayService
import de.tectoast.emolga.utils.Language
import org.koin.core.annotation.Single

@Single
class MonNameProviderFactory(private val displayService: PokemonDisplayService) {
    fun getMonNameProvider(guildId: Long, language: Language): MonNameProvider {
        return DisplayMonNameProvider(displayService, guildId, language)
    }
}