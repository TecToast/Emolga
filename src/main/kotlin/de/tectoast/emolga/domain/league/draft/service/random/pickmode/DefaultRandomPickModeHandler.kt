package de.tectoast.emolga.domain.league.draft.service.random.pickmode

import de.tectoast.emolga.domain.league.draft.model.core.DraftRunContext
import de.tectoast.emolga.domain.league.draft.model.core.ValidationRelevantData
import de.tectoast.emolga.domain.league.draft.model.random.RandomPickMode
import de.tectoast.emolga.domain.league.draft.model.random.RandomPickUserInput
import de.tectoast.emolga.domain.league.draft.service.random.RandomPickChooseService
import de.tectoast.emolga.domain.league.draft.service.random.RandomPickTierParseService
import de.tectoast.emolga.domain.league.tierlist.repository.TierlistRepository
import de.tectoast.emolga.domain.league.tierlist.service.action.dispatcher.TierlistActionDispatcher
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.features.league.draft.K18n_RandomPick
import de.tectoast.emolga.utils.CalcResult
import de.tectoast.emolga.utils.error
import de.tectoast.emolga.utils.getOrReturn
import de.tectoast.emolga.utils.success
import org.koin.core.annotation.Single

@Single
class DefaultRandomPickModeHandler(
    private val dispatcher: TierlistActionDispatcher,
    private val tierParseService: RandomPickTierParseService,
    private val tierlistRepo: TierlistRepository,
    private val randomPickChooseService: RandomPickChooseService,
) :
    RandomPickModeHandler<RandomPickMode.Default> {
    override val targetClass = RandomPickMode.Default::class

    override suspend fun getRandomPick(
        config: RandomPickMode.Default,
        ctx: DraftRunContext,
        input: RandomPickUserInput,
        validationRelevantData: ValidationRelevantData,
    ): CalcResult<Pair<ShowdownID, String>> {
        if (config.tierRequired && input.tier == null) return K18n_RandomPick.TierRequired.error()
        val meta = ctx.tierlistMeta
        val tierlistConfig = meta.config
        val randomPickConfig = ctx.config.randomPick
        val tier = if (input.ignoreRestrictions) input.tier!! else {
            tierParseService.parseTier(
                input.tier,
                randomPickConfig,
                tierlistConfig,
                validationRelevantData.picks
            ).getOrReturn { return it }
        }
        val dbTier = dispatcher.publicTierToDBTier(tierlistConfig, tier)
        val list = tierlistRepo.getByTier(meta.guild, meta.identifier, dbTier).shuffled()
        val showdownID = randomPickChooseService.choose(
            list = list,
            tier = tier,
            leagueName = ctx.league.leagueName,
            input = input,
            validationRelevantData = validationRelevantData,
            tierlistConfig = tierlistConfig,
            doTypeCheck = config.typeAllowed
        )
        return showdownID?.let { (it to tier).success() } ?: K18n_RandomPick.NoPokemonWithTypeAvailable.error()
    }
}
