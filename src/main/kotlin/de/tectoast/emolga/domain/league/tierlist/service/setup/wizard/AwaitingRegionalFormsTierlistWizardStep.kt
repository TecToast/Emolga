package de.tectoast.emolga.domain.league.tierlist.service.setup.wizard

import de.tectoast.emolga.domain.league.tierlist.model.setup.TierlistWizardInput
import de.tectoast.emolga.domain.league.tierlist.model.setup.TierlistWizardState
import de.tectoast.emolga.domain.league.tierlist.service.setup.PokemonAliasService
import de.tectoast.emolga.domain.pokemon.service.PokemonResolverService
import de.tectoast.emolga.features.league.K18n_PrepareTierlist
import de.tectoast.emolga.utils.wizard.WizardStepResult
import org.koin.core.annotation.Single

@Single(binds = [TierlistWizardStep::class])
class AwaitingRegionalFormsTierlistWizardStep(
    private val pokemonResolverService: PokemonResolverService,
    private val pokemonAliasService: PokemonAliasService
) :
    TierlistWizardStep<TierlistWizardState.AwaitingRegionalForms, TierlistWizardInput.RegionalForms>(
        TierlistWizardState.AwaitingRegionalForms::class,
        TierlistWizardInput.RegionalForms::class
    ) {
    override suspend fun execute(
        state: TierlistWizardState.AwaitingRegionalForms,
        input: TierlistWizardInput.RegionalForms
    ): WizardStepResult<TierlistWizardState> {
        val regionalForms = input.regionalForms
        for ((display, official) in regionalForms) {
            if (!display.containsOnePlaceholder() || !official.containsOnePlaceholder()) {
                return WizardStepResult(
                    state,
                    K18n_PrepareTierlist.InvalidRegionalPlaceholder
                )
            }
        }
        val allPokemon = state.pokemonList.flatten()
        var unresolved = pokemonResolverService.getUnresolvedPokemon(state.guild, allPokemon)
        if (unresolved.isNotEmpty() && regionalForms.isNotEmpty()) {
            pokemonAliasService.addRegionalAliases(state.guild, regionalForms, unresolved)
            unresolved = pokemonResolverService.getUnresolvedPokemon(state.guild, unresolved)
        }
        if (unresolved.isNotEmpty()) {
            return WizardStepResult(
                TierlistWizardState.AwaitingPokemonResolution(
                    state.guild,
                    state.pokemonList,
                    unresolved
                )
            )
        }
        return WizardStepResult(
            TierlistWizardState.AwaitingConfig(
                state.guild,
                pokemonResolverService.resolveNested(state.guild, state.pokemonList)
            )
        )
    }

    private fun String.containsOnePlaceholder() = split("***").size == 2
}