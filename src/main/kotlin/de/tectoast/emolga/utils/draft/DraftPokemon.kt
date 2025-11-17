package de.tectoast.emolga.utils.draft

import de.tectoast.emolga.utils.json.Config
import kotlinx.serialization.Serializable

@Serializable
@Config("DraftPokemon", "Ein gepicktes Pokemon")
data class DraftPokemon(
    @Config("Name", "Der Name des Pokemons")
    var name: String = "NAME",
    @Config("Tier", "Das Tier des Pokemons")
    var tier: String = "TIER",
    @Config("Free", "Ob das Pokemon ein Freepick ist")
    var free: Boolean = false,
    var quit: Boolean = false,
    var noCost: Boolean = false
)
