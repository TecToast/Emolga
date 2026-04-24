package de.tectoast.emolga.database.league

import de.tectoast.emolga.database.exposed.*
import de.tectoast.emolga.league.K18n_League
import de.tectoast.emolga.league.TierData
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.draft.K18n_DraftUtils
import de.tectoast.emolga.utils.json.CalcResult
import de.tectoast.emolga.utils.json.error
import de.tectoast.emolga.utils.json.getOrReturn
import de.tectoast.emolga.utils.json.success

class DraftValidationService(
    val picksRepo: LeaguePickRepository,
    val tierlistService: TierlistService,
    val priceConfigDispatcher: TierlistPriceConfigDispatcher,
    val banRoundConfigDispatcher: BanRoundConfigDispatcher
) {
    suspend fun validateDraftInput(
        ctx: DraftRunContext, input: DraftInput, type: DraftMessageType, alreadyPicked: Set<String>
    ): CalcResult<ValidationSuccess> {
        if (input.pokemon.showdownId in alreadyPicked) return K18n_DraftUtils.PokemonAlreadyPicked.error()
        val leagueName = ctx.league.leagueName
        val picks = picksRepo.getPicksForUser(leagueName, ctx.activeIdx)
        return when (input) {
            is PickInput -> validatePickInput(ctx, input, type, picks)
            is SwitchInput -> validateSwitchInput(ctx, input, type, picks)
            is BanInput -> validateBanInput(ctx, input, type, picks)
        }
    }

    private suspend fun validatePickInput(
        ctx: DraftRunContext, input: PickInput, type: DraftMessageType, picks: List<DraftPokemon>
    ): CalcResult<ValidationSuccess> {
        val (leagueData, config, tl, idx) = ctx
        if (type != DraftMessageType.RANDOM && config.randomPick.hasJokers()) return K18n_League.LegalRandomPickObligatory.error()
        config.draftBan?.let {
            it.banRounds[leagueData.round]?.let {
                return K18n_League.LegalActionIsNotBan(leagueData.round).error()
            }
        }
        if (picks.count { !it.quit } >= config.teamSize) return K18n_League.TeamFull.error()
        if (leagueData.isSwitchDraft && !config.triggers.allowPickDuringSwitch) return K18n_DraftUtils.NoPickDuringSwitch.error()
        val teraConfig = config.teraPick
        val isTeraPick = input.tera && teraConfig != null
        if (isTeraPick && picks.count { it.tera } >= teraConfig.amount) {
            return K18n_DraftUtils.TeraUserAlreadyPicked.error()
        }
        val identifier = if (isTeraPick) teraConfig.tlIdentifier else config.tlIdentifier
        val official = input.pokemon.showdownId
        val tierData = tierlistService.getTierData(tl, official, input.tier, identifier).getOrReturn { return it }
        val context = DraftActionContext()
        with(ValidationRelevantData(picks = picks, idx = idx, teamSize = config.teamSize)) {
            priceConfigDispatcher.handleDraftActionWithGeneralChecks(
                tl.priceConfig, DraftAction(
                    tier = tierData, official = official, free = input.free, tera = input.tera, switch = null
                ), context
            )
        }?.let { return it.error() }
        val saveTier = context.saveTier ?: tierData.specified
        return ValidationSuccess(
            saveTier = saveTier, freePick = context.freePick, updrafted = saveTier != tierData.official
        ).success()
    }

    private suspend fun validateSwitchInput(
        ctx: DraftRunContext, input: SwitchInput, type: DraftMessageType, picks: List<DraftPokemon>
    ): CalcResult<ValidationSuccess> {
        val (leagueData, config, tl, idx) = ctx
        config.draftBan?.let {
            it.banRounds[leagueData.round]?.let {
                return K18n_League.LegalActionIsNotBan(leagueData.round).error()
            }
        }
        if (!leagueData.isSwitchDraft) return K18n_DraftUtils.NoSwitchAvailable.error()
        val oldDraftMon =
            picks.firstOrNull { it.name == input.oldmon } ?: return K18n_DraftUtils.PokemonNotInYourTeam(
                input.oldmon // TODO tl name here
            ).error()
        val official = input.pokemon.showdownId
        val tierData = tierlistService.getTierData(tl, official, null).getOrReturn { return it }
        with(ValidationRelevantData(picks = picks, idx = idx, teamSize = config.teamSize)) {
            priceConfigDispatcher.handleDraftActionWithGeneralChecks(
                tl.priceConfig, DraftAction(
                    tier = tierData, official = official, free = false, tera = false, switch = oldDraftMon
                ), null
            )?.let { return it.error() }
        }
        return ValidationSuccess(saveTier = tierData.specified, freePick = false, updrafted = false).success()
    }

    private suspend fun validateBanInput(
        ctx: DraftRunContext, input: BanInput, type: DraftMessageType, picks: List<DraftPokemon>
    ): CalcResult<ValidationSuccess> {
        val (leagueData, config, tl, _) = ctx
        val draftBanConfig = config.draftBan ?: return K18n_DraftUtils.BanNotEnabled.error()
        val pokemon = input.pokemon
        val official = pokemon.showdownId
        if (official in draftBanConfig.notBannable) return K18n_DraftUtils.BanNotPossibleForMon(pokemon.tlName).error()

        val banRoundConfig =
            draftBanConfig.banRounds[leagueData.round] ?: return K18n_DraftUtils.NoBanRound(leagueData.round).error()
        val tier = tierlistService.getTierData(tl, official, requestedTier = null)
            .getOrReturn<TierData, ValidationSuccess> { return it }.official
        banRoundConfigDispatcher.checkBan(banRoundConfig, tier, leagueData.alreadyBannedMonsThisRound)?.let { reason ->
            return reason.error()
        }
        return ValidationSuccess(saveTier = tier, freePick = false, updrafted = false).success()
    }
}

data class ValidationSuccess(val saveTier: String, val freePick: Boolean, val updrafted: Boolean)
