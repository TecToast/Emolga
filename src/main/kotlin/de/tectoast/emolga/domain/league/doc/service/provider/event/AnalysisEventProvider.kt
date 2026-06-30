package de.tectoast.emolga.domain.league.doc.service.provider.event

import de.tectoast.emolga.domain.game.service.process.analysis.AnalysisEvents
import de.tectoast.emolga.domain.league.gamedata.model.GameData

interface AnalysisEventProvider {
    fun getEvents(gameData: GameData): AnalysisEvents
}