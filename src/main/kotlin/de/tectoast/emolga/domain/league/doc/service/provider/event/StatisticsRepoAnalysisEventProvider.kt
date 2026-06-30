package de.tectoast.emolga.domain.league.doc.service.provider.event

import de.tectoast.emolga.domain.game.service.process.analysis.AnalysisEvents
import de.tectoast.emolga.domain.league.gamedata.model.GameData
import de.tectoast.emolga.domain.statistics.repository.StatisticsRepository
import org.koin.core.annotation.Single

@Single
class StatisticsRepoAnalysisEventProvider(private val statisticsRepo: StatisticsRepository) : AnalysisEventProvider {
    override fun getEvents(gameData: GameData): AnalysisEvents {
        return statisticsRepo.getEvents(gameData.url) ?: AnalysisEvents()
    }
}