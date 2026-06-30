package de.tectoast.emolga.domain.league.tierlist.model.setup

import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface TierlistWizardState {
    @Serializable
    @SerialName("AwaitingPokemonList")
    data object AwaitingPokemonList : TierlistWizardState

    @Serializable
    @SerialName("AwaitingRegionalForms")
    data class AwaitingRegionalForms(val guild: Long, val pokemonList: List<List<String>>) : TierlistWizardState

    @Serializable
    @SerialName("AwaitingPokemonResolution")
    data class AwaitingPokemonResolution(
        val guild: Long,
        val pokemonList: List<List<String>>,
        val unresolved: Set<String>
    ) :
        TierlistWizardState

    @Serializable
    @SerialName("AwaitingConfig")
    data class AwaitingConfig(val guild: Long, val pokemonList: List<List<ShowdownID>>) : TierlistWizardState
}