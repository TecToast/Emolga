package de.tectoast.emolga.domain.league.draft.service.random

import de.tectoast.emolga.domain.league.draft.model.core.DraftAction
import de.tectoast.emolga.domain.league.draft.model.core.ValidationRelevantData
import de.tectoast.emolga.domain.league.draft.model.random.RandomPickUserInput
import de.tectoast.emolga.domain.league.draft.repository.LeaguePickRepository
import de.tectoast.emolga.domain.league.tierlist.model.config.TierlistConfig
import de.tectoast.emolga.domain.league.tierlist.service.action.dispatcher.TierlistActionDispatcher
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.domain.pokemon.repository.PokedexRepository
import org.koin.core.annotation.Single

@Single
class RandomPickChooseService(
    private val leaguePickRepo: LeaguePickRepository,
    private val dispatcher: TierlistActionDispatcher,
    private val pokedexRepo: PokedexRepository
) {
    suspend fun choose(
        list: List<ShowdownID>,
        tier: String,
        leagueName: String,
        input: RandomPickUserInput,
        validationRelevantData: ValidationRelevantData,
        tierlistConfig: TierlistConfig,
        doTypeCheck: Boolean
    ): ShowdownID? {
        val alreadyPicked = leaguePickRepo.getAllPickedIds(leagueName)
        return list.firstNotNullOfOrNull l@{ showdownID ->
            if (showdownID in alreadyPicked) return@l null
            if (showdownID in input.skipMons) return@l null
            with(validationRelevantData) {
                dispatcher.handleDraftActionWithGeneralChecks(
                    tierlistConfig,
                    DraftAction(showdownID, tier)
                )?.let { return@l null }
            }
            if (doTypeCheck && input.type != null && input.type !in pokedexRepo.get(showdownID)!!.types) return@l null
            return@l showdownID
        }
    }
}