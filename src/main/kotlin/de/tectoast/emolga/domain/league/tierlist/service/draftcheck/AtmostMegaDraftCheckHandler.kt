package de.tectoast.emolga.domain.league.tierlist.service.draftcheck

import de.tectoast.emolga.domain.league.draft.model.core.DraftAction
import de.tectoast.emolga.domain.league.draft.model.core.ValidationRelevantData
import de.tectoast.emolga.domain.league.tierlist.model.DraftCheck
import de.tectoast.emolga.domain.pokemon.repository.PokedexRepository
import de.tectoast.emolga.utils.ErrorOrNull
import de.tectoast.emolga.utils.draft.K18n_Tierlist
import org.koin.core.annotation.Single

@Single
class AtmostMegaDraftCheckHandler(private val pokedexRepo: PokedexRepository) :
    DraftCheckHandler<DraftCheck.AtmostMega> {
    override val targetClass = DraftCheck.AtmostMega::class

    context(data: ValidationRelevantData)
    override suspend fun check(config: DraftCheck.AtmostMega, action: DraftAction): ErrorOrNull {
        val isMega = pokedexRepo.isMega(action.showdownId)
        if (isMega && data.picks.count { pokedexRepo.isMega(it.showdownId) } >= config.count) {
            return K18n_Tierlist.AtmostMega(config.count)
        }
        return null
    }
}