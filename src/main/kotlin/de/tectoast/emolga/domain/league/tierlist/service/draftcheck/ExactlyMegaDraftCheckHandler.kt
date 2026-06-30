package de.tectoast.emolga.domain.league.tierlist.service.draftcheck

import de.tectoast.emolga.domain.league.draft.model.core.DraftAction
import de.tectoast.emolga.domain.league.draft.model.core.ValidationRelevantData
import de.tectoast.emolga.domain.league.tierlist.model.DraftCheck
import de.tectoast.emolga.domain.pokemon.repository.PokedexRepository
import de.tectoast.emolga.utils.ErrorOrNull
import de.tectoast.emolga.utils.draft.K18n_Tierlist
import org.koin.core.annotation.Single

@Single
class ExactlyMegaDraftCheckHandler(private val pokedexRepo: PokedexRepository) :
    DraftCheckHandler<DraftCheck.ExactlyMega> {
    override val targetClass = DraftCheck.ExactlyMega::class

    context(data: ValidationRelevantData)
    override suspend fun check(
        config: DraftCheck.ExactlyMega,
        action: DraftAction
    ): ErrorOrNull {
        val isMega = pokedexRepo.isMega(action.showdownId)
        val picks = data.picks
        val megaCount = picks.count { pokedexRepo.isMega(it.showdownId) }
        if (isMega && megaCount >= config.count) {
            return K18n_Tierlist.ExactlyMega(config.count)
        }
        if (!isMega && megaCount < config.count && picks.size == data.teamSize - config.count) {
            return K18n_Tierlist.ExactlyMega(config.count)
        }
        return null
    }
}