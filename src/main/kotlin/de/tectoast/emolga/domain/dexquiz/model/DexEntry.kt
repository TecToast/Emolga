package de.tectoast.emolga.domain.dexquiz.model

import de.tectoast.emolga.domain.pokemon.model.ShowdownID

data class DexEntry(val id: Int, val showdownId: ShowdownID, val games: List<String>, val entry: String)