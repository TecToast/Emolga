package de.tectoast.emolga.domain.game.model.analysis

import de.tectoast.emolga.domain.game.service.process.analysis.BattleContext
import de.tectoast.emolga.domain.game.service.process.analysis.SDPlayer

data class AnalysisData(
    val game: List<SDPlayer>, val ctx: BattleContext, val dontTranslate: Boolean
)