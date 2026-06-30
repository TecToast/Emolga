package de.tectoast.emolga.domain.league.draft.model.execution

import de.tectoast.emolga.domain.pokemon.model.ShowdownID

data class SnipeMeta(val disableIfSniped: Boolean, val list: MutableList<SnipeData>)
data class SnipeData(val pokemon: ShowdownID, val sniper: Int)
