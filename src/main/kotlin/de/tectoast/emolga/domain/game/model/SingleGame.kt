package de.tectoast.emolga.domain.game.model

import de.tectoast.emolga.domain.pokemon.model.ShowdownID

data class SingleGame(
    val source: GameSource,
    val is4v4: Boolean,
    val winnerIndex: Int,
    val kd: List<List<KDWithName>>,
    val defaultNameLookup: Map<ShowdownID, String> = emptyMap()
)