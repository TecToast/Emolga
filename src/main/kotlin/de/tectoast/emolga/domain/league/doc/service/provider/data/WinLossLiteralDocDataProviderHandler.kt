package de.tectoast.emolga.domain.league.doc.service.provider.data

import de.tectoast.emolga.domain.league.doc.model.AdditionalDataProvider
import de.tectoast.emolga.domain.league.doc.model.DocDataProviderConfig
import de.tectoast.emolga.domain.league.doc.model.StatProcessorData
import de.tectoast.emolga.domain.league.gamedata.model.FullGameData
import org.koin.core.annotation.Single

@Single
class WinLossLiteralHandler : DocDataProviderHandler<DocDataProviderConfig.WinLossLiteral> {
    override val targetClass = DocDataProviderConfig.WinLossLiteral::class

    override suspend fun provideData(
        config: DocDataProviderConfig.WinLossLiteral,
        fullGameData: FullGameData,
        statProcessorData: StatProcessorData,
        provider: AdditionalDataProvider
    ): Any {
        val (wins, losses) = fullGameData.getWinsLosses(statProcessorData)
        return if (wins > losses) config.win else config.loss
    }
}