package de.tectoast.emolga.domain.league.draft.service.random.pickmode

import de.tectoast.emolga.domain.league.draft.model.core.DraftRunContext
import de.tectoast.emolga.domain.league.draft.model.core.ValidationRelevantData
import de.tectoast.emolga.domain.league.draft.model.random.RandomPickMode
import de.tectoast.emolga.domain.league.draft.model.random.RandomPickUserInput
import de.tectoast.emolga.domain.league.draft.service.random.RandomPickChooseService
import de.tectoast.emolga.domain.league.tierlist.model.config.TierBasedTierlistConfig
import de.tectoast.emolga.domain.league.tierlist.repository.TierlistRepository
import de.tectoast.emolga.domain.league.tierlist.service.action.dispatcher.TierBasedTierlistActionDispatcher
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.features.league.draft.K18n_RandomPick
import de.tectoast.emolga.utils.BotConstants
import de.tectoast.emolga.utils.CalcResult
import de.tectoast.emolga.utils.error
import de.tectoast.emolga.utils.success
import mu.KotlinLogging
import org.koin.core.annotation.Single


@Single
class TypeTierlistRandomPickModeHandler(
    private val tierBasedDispatcher: TierBasedTierlistActionDispatcher,
    private val tierlistRepo: TierlistRepository,
    private val randomPickChooseService: RandomPickChooseService,
    private val botConstants: BotConstants
) : RandomPickModeHandler<RandomPickMode.TypeTierlist> {
    override val targetClass = RandomPickMode.TypeTierlist::class

    private val logger = KotlinLogging.logger {}

    override suspend fun getRandomPick(
        config: RandomPickMode.TypeTierlist,
        ctx: DraftRunContext,
        input: RandomPickUserInput,
        validationRelevantData: ValidationRelevantData,
    ): CalcResult<Pair<ShowdownID, String>> {
        val type = input.type ?: return K18n_RandomPick.TypeRequired.error()
        val picks = validationRelevantData.picks
        var result: Pair<ShowdownID, String>? = null
        val usedTiers = mutableSetOf<String>()
        val meta = ctx.tierlistMeta
        val tierlistConfig = meta.config
        if (tierlistConfig !is TierBasedTierlistConfig) return K18n_RandomPick.TypeTierlistNotSupported.error()
        val prices = tierBasedDispatcher.getSingleMap(tierlistConfig)
        run {
            repeat(prices.size) { _ ->
                val temptier =
                    prices.filter { (tier, amount) -> tier !in usedTiers && picks.count { mon -> mon.tier == tier } < amount }.keys.randomOrNull()
                        ?: return K18n_RandomPick.TypeDoesntMatch(type).error()
                val list = tierlistRepo.getByTierAndType(meta.guild, meta.identifier, temptier, type).shuffled()
                val tempmon = randomPickChooseService.choose(
                    list = list,
                    tier = temptier,
                    leagueName = ctx.league.leagueName,
                    input = input,
                    validationRelevantData = validationRelevantData,
                    tierlistConfig = tierlistConfig,
                    doTypeCheck = false
                )
                if (tempmon != null) {
                    result = tempmon to temptier
                    return@run
                }
                usedTiers += temptier
            }
        }
        if (result == null) {
            logger.error("No pokemon found without error message: ${validationRelevantData.idx} $type")
            return K18n_RandomPick.NoPokemonFound(botConstants.botOwnerId).error()
        }
        return result.success()
    }


}
