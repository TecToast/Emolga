package de.tectoast.emolga.domain.league.doc.model

import de.tectoast.emolga.domain.eventbus.EmolgaEvent
import de.tectoast.emolga.domain.game.model.FullInputGame

data class HideGamesInsertData(
    val games: List<FullInputGame>,
    val guild: Long,
    val leagueName: String
) :
    EmolgaEvent
