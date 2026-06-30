package de.tectoast.emolga.domain.league.doc.model

import de.tectoast.emolga.domain.game.model.FullInputGame

data class HideGamesInsertData(val games: List<FullInputGame>, val hideGamesConfig: HideGamesConfig, val guild: Long)