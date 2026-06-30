package de.tectoast.emolga.domain.league.teamgraphic.model

import de.tectoast.emolga.domain.pokemon.model.ShowdownID

data class DrawData(val name: ShowdownID, val x: Int, val y: Int, val size: Int, val flipped: Boolean)
