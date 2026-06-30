package de.tectoast.emolga.domain.league.doc.model

import de.tectoast.emolga.domain.game.service.process.analysis.AnalysisEvents
import de.tectoast.emolga.domain.pokemon.model.ShowdownID

data class StatisticSource(val events: AnalysisEvents, val showdownId: ShowdownID, val indexInData: Int)