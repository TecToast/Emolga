package de.tectoast.emolga.domain.league.tierlist.model.setup

import de.tectoast.emolga.domain.league.tierlist.model.config.TierlistConfig
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.utils.Language

sealed interface TierlistWizardInput {
    data class PokemonList(val guild: Long, val columns: List<List<String>>) : TierlistWizardInput
    data class RegionalForms(val regionalForms: Map<String, String>) : TierlistWizardInput
    data class PokemonResolution(val resolutions: Map<String, ShowdownID>) : TierlistWizardInput
    data class Config(
        val identifier: String,
        val config: TierlistConfig,
        val language: Language,
        val teamSize: Int,
        val tierAssociation: Map<Int, String>?
    ) :
        TierlistWizardInput
}