package de.tectoast.emolga.domain.league.tierlist.service.setup.wizard

import de.tectoast.emolga.domain.league.tierlist.model.setup.TierlistWizardInput
import de.tectoast.emolga.domain.league.tierlist.model.setup.TierlistWizardState
import de.tectoast.emolga.domain.league.tierlist.service.setup.PokemonAliasService
import de.tectoast.emolga.domain.pokemon.service.PokemonResolverService
import de.tectoast.emolga.utils.wizard.WizardStepResult
import org.koin.core.annotation.Single

@Single(binds = [TierlistWizardStep::class])
class AwaitingPokemonResolutionTierlistWizardStep(
    private val pokemonAliasService: PokemonAliasService,
    private val pokemonResolverService: PokemonResolverService
) :
    TierlistWizardStep<TierlistWizardState.AwaitingPokemonResolution, TierlistWizardInput.PokemonResolution>(
        TierlistWizardState.AwaitingPokemonResolution::class, TierlistWizardInput.PokemonResolution::class
    ) {

    override suspend fun execute(
        state: TierlistWizardState.AwaitingPokemonResolution, input: TierlistWizardInput.PokemonResolution
    ): WizardStepResult<TierlistWizardState> {
        pokemonAliasService.addExplicitAliases(state.guild, input.resolutions)
        val unresolved = pokemonResolverService.getUnresolvedPokemon(state.guild, state.unresolved)
        if (unresolved.isNotEmpty()) {
            return WizardStepResult(
                state.copy(unresolved = unresolved)
            )
        }
        return WizardStepResult(
            TierlistWizardState.AwaitingConfig(
                state.guild,
                pokemonResolverService.resolveNested(state.guild, state.pokemonList)
            )
        )
    }
}