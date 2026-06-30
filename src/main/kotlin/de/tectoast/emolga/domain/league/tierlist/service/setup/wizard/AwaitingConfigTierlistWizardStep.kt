package de.tectoast.emolga.domain.league.tierlist.service.setup.wizard

import de.tectoast.emolga.domain.league.tierlist.model.TierlistMeta
import de.tectoast.emolga.domain.league.tierlist.model.setup.TierlistWizardInput
import de.tectoast.emolga.domain.league.tierlist.model.setup.TierlistWizardState
import de.tectoast.emolga.domain.league.tierlist.repository.TierlistRepository
import de.tectoast.emolga.domain.league.tierlist.service.action.dispatcher.TierlistActionDispatcher
import de.tectoast.emolga.utils.wizard.WizardStepResult
import org.koin.core.annotation.Single

@Single(binds = [TierlistWizardStep::class])
class AwaitingConfigTierlistWizardStep(
    val tierlistRepo: TierlistRepository,
    val tierlistActionDispatcher: TierlistActionDispatcher
) :
    TierlistWizardStep<TierlistWizardState.AwaitingConfig, TierlistWizardInput.Config>(
        TierlistWizardState.AwaitingConfig::class,
        TierlistWizardInput.Config::class
    ) {
    override suspend fun execute(
        state: TierlistWizardState.AwaitingConfig,
        input: TierlistWizardInput.Config
    ): WizardStepResult<TierlistWizardState>? {
        val config = input.config
        val meta = TierlistMeta(state.guild, input.identifier, input.language, config)
        val pokemonList = state.pokemonList
        val columnCount = pokemonList.size
        val tierMapping = input.tierAssociation ?: buildMap {
            val allTiers = tierlistActionDispatcher.getTiers(config)
            val lastTier = allTiers.last()
            allTiers.forEachIndexed { index, tier ->
                put(index, tier)
            }
            for (i in allTiers.size..<columnCount) {
                put(i, lastTier)
            }
        }
        tierlistRepo.upsertMeta(meta)
        tierlistRepo.setTierlistEntries(
            state.guild,
            input.identifier,
            pokemonList.flatMapIndexed { index, ids ->
                ids.map { id -> id to tierMapping[index]!! }
            })
        return null
    }
}