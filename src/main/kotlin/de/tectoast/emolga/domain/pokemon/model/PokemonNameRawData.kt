package de.tectoast.emolga.domain.pokemon.model

data class PokemonNameRawData(
    val showdownId: ShowdownID, val nameEn: String, val nameDe: String, val customGuildName: String?
)