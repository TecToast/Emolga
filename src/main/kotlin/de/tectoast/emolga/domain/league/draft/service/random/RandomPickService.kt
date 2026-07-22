package de.tectoast.emolga.domain.league.draft.service.random

import de.tectoast.emolga.domain.league.draft.model.core.DraftRunContext
import de.tectoast.emolga.domain.league.draft.model.core.ValidationRelevantData
import de.tectoast.emolga.domain.league.draft.model.random.RandomLeaguePick
import de.tectoast.emolga.domain.league.draft.model.random.RandomPickResult
import de.tectoast.emolga.domain.league.draft.model.random.RandomPickUserInput
import de.tectoast.emolga.domain.league.draft.repository.LeaguePickRepository
import de.tectoast.emolga.domain.league.draft.service.random.pickmode.RandomPickModeDispatcher
import de.tectoast.emolga.domain.league.draft.util.getDisplayName
import de.tectoast.emolga.domain.pokemon.service.PokemonDisplayService
import de.tectoast.emolga.league.K18n_League
import de.tectoast.emolga.utils.CalcResult
import de.tectoast.emolga.utils.error
import de.tectoast.emolga.utils.getOrReturn
import de.tectoast.emolga.utils.success
import org.koin.core.annotation.Single

@Single
class RandomPickService(
    private val dispatcher: RandomPickModeDispatcher,
    private val pokemonDisplayService: PokemonDisplayService,
    private val leaguePickRepo: LeaguePickRepository
) {
    suspend fun getRandomPick(input: RandomPickUserInput, ctx: DraftRunContext): CalcResult<RandomPickResult> {
        val config = ctx.config.randomPick
        if (!config.enabled) return K18n_League.RandomPickDisabled.error()
        val leagueHasJokers = config.hasJokers()
        val randomPickDraftData = ctx.league.draftData.randomPick
        if (leagueHasJokers && randomPickDraftData.currentMon?.disabled == false) return K18n_League.RandomPickAlreadyGambled.error()
        val validationRelevantData = ValidationRelevantData(
            picks = leaguePickRepo.getPicksForUser(ctx.league.leagueName, ctx.activeIdx),
            idx = ctx.activeIdx,
            teamSize = ctx.tierlistMeta.teamSize
        )
        val (showdownId, tier) = dispatcher.getRandomPick(config.mode, ctx, input, validationRelevantData)
            .getOrReturn { return it }
        if (leagueHasJokers) {
            val jokerAmount = config.jokers - (randomPickDraftData.usedJokers[ctx.activeIdx] ?: 0)
            if (jokerAmount > 0) {
                randomPickDraftData.currentMon = RandomLeaguePick(
                    showdownId = showdownId,
                    tier = tier,
                    free = input.free,
                    tera = input.tera,
                    data = mapOf("type" to input.type),
                    history = randomPickDraftData.currentMon?.history.orEmpty() + showdownId,
                )
                return RandomPickResult.RerollPossible(
                    showdownId = showdownId,
                    tier = tier,
                    displayName = pokemonDisplayService.getDisplayName(showdownId, ctx),
                    jokersRemaining = jokerAmount
                ).success()
            }
        }
        return RandomPickResult.NoReroll(showdownId, tier).success()
    }
}
