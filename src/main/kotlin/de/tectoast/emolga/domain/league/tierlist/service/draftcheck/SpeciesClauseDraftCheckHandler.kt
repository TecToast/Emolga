package de.tectoast.emolga.domain.league.tierlist.service.draftcheck

import de.tectoast.emolga.domain.league.draft.model.core.DraftAction
import de.tectoast.emolga.domain.league.draft.model.core.ValidationRelevantData
import de.tectoast.emolga.domain.league.tierlist.model.DraftCheck
import de.tectoast.emolga.domain.pokemon.repository.PokedexRepository
import de.tectoast.emolga.utils.ErrorOrNull
import de.tectoast.emolga.utils.draft.K18n_Tierlist
import org.koin.core.annotation.Single

@Single
class SpeciesClauseDraftCheckHandler(private val pokedexRepository: PokedexRepository) :
    DraftCheckHandler<DraftCheck.SpeciesClause> {
    override val targetClass = DraftCheck.SpeciesClause::class

    context(data: ValidationRelevantData)
    override suspend fun check(config: DraftCheck.SpeciesClause, action: DraftAction): ErrorOrNull {

        val existingDexNumbers = pokedexRepository.getPokedexNumbers(data.picks.map { it.showdownId }).values.toSet()
        val actionDexNumber = pokedexRepository.getPokedexNumber(action.showdownId)
        if (actionDexNumber in existingDexNumbers) {
            return K18n_Tierlist.SpeciesClause
        }
        return null
    }
}