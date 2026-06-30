package de.tectoast.emolga.domain.league.tierlist.service.setup.wizard

import de.tectoast.emolga.domain.league.tierlist.model.setup.TierlistWizardInput
import de.tectoast.emolga.domain.league.tierlist.model.setup.TierlistWizardState
import de.tectoast.emolga.features.league.K18n_PrepareTierlist
import de.tectoast.emolga.utils.wizard.WizardStepResult
import org.koin.core.annotation.Single

@Single(binds = [TierlistWizardStep::class])
class AwaitingPokemonTierlistWizardStep :
    TierlistWizardStep<TierlistWizardState.AwaitingPokemonList, TierlistWizardInput.PokemonList>(
        TierlistWizardState.AwaitingPokemonList::class,
        TierlistWizardInput.PokemonList::class
    ) {
    override suspend fun execute(
        state: TierlistWizardState.AwaitingPokemonList,
        input: TierlistWizardInput.PokemonList
    ): WizardStepResult<TierlistWizardState> {
        val duplicates = input.columns.flatten().getDuplicates()
        if (duplicates.isNotEmpty()) {
            return WizardStepResult(
                TierlistWizardState.AwaitingPokemonList,
                K18n_PrepareTierlist.DuplicatePokemon(duplicates.joinToString())
            )
        }
        return WizardStepResult(TierlistWizardState.AwaitingRegionalForms(input.guild, input.columns))
    }

    private fun List<String>.getDuplicates(): List<String> {
        return this.groupingBy { it }.eachCount().filter { it.value > 1 }.keys.toList()
    }
}