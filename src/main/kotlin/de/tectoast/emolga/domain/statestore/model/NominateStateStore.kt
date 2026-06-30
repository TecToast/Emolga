package de.tectoast.emolga.domain.statestore.model

import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("NominateState")
class NominateState(
    val nomUser: Int, val originalMons: List<DraftPokemon>, val mons: List<DraftPokemon>
) : StateStore() {
    val nominated: MutableList<DraftPokemon> = ArrayList(mons)
    val notNominated: MutableList<DraftPokemon> = ArrayList(mons.size)
}