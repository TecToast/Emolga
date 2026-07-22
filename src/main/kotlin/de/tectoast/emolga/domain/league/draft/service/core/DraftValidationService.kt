package de.tectoast.emolga.domain.league.draft.service.core

import de.tectoast.emolga.domain.league.draft.model.core.*
import de.tectoast.emolga.domain.league.draft.repository.LeaguePickRepository
import de.tectoast.emolga.domain.league.draft.service.core.ban.BanRoundConfigDispatcher
import de.tectoast.emolga.domain.league.draft.util.getDisplayName
import de.tectoast.emolga.domain.league.tierlist.model.TierData
import de.tectoast.emolga.domain.league.tierlist.model.config.PointBasedTierlistConfig
import de.tectoast.emolga.domain.league.tierlist.service.action.dispatcher.PointBasedTierlistActionDispatcher
import de.tectoast.emolga.domain.league.tierlist.service.action.dispatcher.TierlistActionDispatcher
import de.tectoast.emolga.domain.league.tierlist.service.core.TierDataService
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.domain.pokemon.service.PokemonDisplayService
import de.tectoast.emolga.league.K18n_League
import de.tectoast.emolga.utils.CalcResult
import de.tectoast.emolga.utils.draft.K18n_DraftUtils
import de.tectoast.emolga.utils.error
import de.tectoast.emolga.utils.getOrReturn
import de.tectoast.emolga.utils.success
import org.koin.core.annotation.Single

@Single
class DraftValidationService(
    private val picksRepo: LeaguePickRepository,
    private val tierDataService: TierDataService,
    private val tierlistActionDispatcher: TierlistActionDispatcher,
    private val pointConfigDispatcher: PointBasedTierlistActionDispatcher,
    private val banRoundConfigDispatcher: BanRoundConfigDispatcher,
    private val pokemonDisplayService: PokemonDisplayService,
) {
    suspend fun validateDraftInput(
        ctx: DraftRunContext, input: DraftInput, type: DraftActionOrigin, alreadyPicked: Set<ShowdownID>
    ): CalcResult<ValidationSuccess> {
        if (input.pokemon in alreadyPicked) return K18n_DraftUtils.PokemonAlreadyPicked.error()
        val leagueName = ctx.league.leagueName
        val picks = picksRepo.getPicksForUser(leagueName, ctx.activeIdx)
        return when (input) {
            is PickInput -> validatePickInput(ctx, input, type, picks)
            is SwitchInput -> validateSwitchInput(ctx, input, picks)
            is BanInput -> validateBanInput(ctx, input)
        }
    }

    private suspend fun validatePickInput(
        ctx: DraftRunContext, input: PickInput, type: DraftActionOrigin, picks: List<DraftPokemon>
    ): CalcResult<ValidationSuccess> {
        val (leagueData, config, tl, pickContext) = ctx
        val idx = pickContext.idx
        if (type != DraftActionOrigin.RANDOM && config.randomPick.hasJokers()) return K18n_League.LegalRandomPickObligatory.error()
        config.draftBan?.let {
            it.banRounds[leagueData.round]?.let {
                return K18n_League.LegalActionIsNotBan(leagueData.round).error()
            }
        }
        if (picks.count { !it.quit } >= tl.teamSize) return K18n_League.TeamFull.error()
        if (leagueData.isSwitchDraft && !config.triggers.allowPickDuringSwitch) return K18n_DraftUtils.NoPickDuringSwitch.error()
        val teraConfig = config.teraPick
        val isTeraPick = input.tera && teraConfig != null
        if (isTeraPick && picks.count { it.tera } >= teraConfig.amount) {
            return K18n_DraftUtils.TeraUserAlreadyPicked.error()
        }
        val identifier = if (isTeraPick) teraConfig.tlIdentifier else config.tlIdentifier
        val showdownId = input.pokemon
        val tierData = tierDataService.getTierData(tl, showdownId, input.tier, identifier).getOrReturn { return it }
        val context = DraftActionContext()
        with(ValidationRelevantData(picks = picks, idx = idx, teamSize = tl.teamSize)) {
            tierlistActionDispatcher.handleDraftActionWithGeneralChecks(
                tl.config, DraftAction(
                    tier = tierData, showdownId = showdownId, free = input.free, tera = input.tera
                ), context
            )
        }?.let { return it.error() }
        val saveTier = context.saveTier ?: tierData.specified
        return ValidationSuccess(
            saveTier = saveTier, freePick = context.freePick, updrafted = saveTier != tierData.official,
            points = (tl.config as? PointBasedTierlistConfig)?.let {
                pointConfigDispatcher.getPointsForTier(
                    it,
                    saveTier
                )
            }
        ).success()
    }

    private suspend fun validateSwitchInput(
        ctx: DraftRunContext, input: SwitchInput, picks: List<DraftPokemon>
    ): CalcResult<ValidationSuccess> {
        val (leagueData, config, tl, pickContext) = ctx
        val idx = pickContext.idx
        config.draftBan?.let {
            it.banRounds[leagueData.round]?.let {
                return K18n_League.LegalActionIsNotBan(leagueData.round).error()
            }
        }
        if (!leagueData.isSwitchDraft) return K18n_DraftUtils.NoSwitchAvailable.error()
        val oldDraftMon =
            picks.firstOrNull { it.showdownId == input.oldmon } ?: return K18n_DraftUtils.PokemonNotInYourTeam(
                pokemonDisplayService.getDisplayName(input.oldmon, ctx)
            ).error()
        val official = input.pokemon
        val tierData = tierDataService.getTierData(tl, official, null).getOrReturn { return it }
        with(ValidationRelevantData(picks = picks, idx = idx, teamSize = tl.teamSize)) {
            tierlistActionDispatcher.handleDraftActionWithGeneralChecks(
                tl.config, DraftAction(
                    tier = tierData, showdownId = official, switch = oldDraftMon
                )
            )?.let { return it.error() }
        }
        return ValidationSuccess(
            saveTier = tierData.specified,
            freePick = false,
            updrafted = false,
            points = null
        ).success()
    }

    private suspend fun validateBanInput(
        ctx: DraftRunContext, input: BanInput
    ): CalcResult<ValidationSuccess> {
        val (leagueData, config, tl, _) = ctx
        val draftBanConfig = config.draftBan ?: return K18n_DraftUtils.BanNotEnabled.error()
        val pokemon = input.pokemon
        if (pokemon in draftBanConfig.notBannable) return K18n_DraftUtils.BanNotPossibleForMon(
            pokemonDisplayService.getDisplayName(
                pokemon,
                ctx
            )
        ).error()
        val banRoundConfig =
            draftBanConfig.banRounds[leagueData.round] ?: return K18n_DraftUtils.NoBanRound(leagueData.round).error()
        val tier = tierDataService.getTierData(tl, pokemon, requestedTier = null)
            .getOrReturn<TierData, ValidationSuccess> { return it }.official
        banRoundConfigDispatcher.checkBan(banRoundConfig, tier, leagueData.alreadyBannedMonsThisRound)?.let { reason ->
            return reason.error()
        }
        return ValidationSuccess(saveTier = tier, freePick = false, updrafted = false, points = null).success()
    }
}

