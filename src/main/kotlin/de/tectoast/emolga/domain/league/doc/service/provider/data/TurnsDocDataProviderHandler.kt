package de.tectoast.emolga.domain.league.doc.service.provider.data

import de.tectoast.emolga.domain.league.doc.model.AdditionalDataProvider
import de.tectoast.emolga.domain.league.doc.model.DocDataProviderConfig
import de.tectoast.emolga.domain.league.doc.model.StatProcessorData
import de.tectoast.emolga.domain.league.gamedata.model.FullGameData
import org.koin.core.annotation.Single

@Single
class TurnsDocDataProviderHandler : DocDataProviderHandler<DocDataProviderConfig.Turns> {
    override val targetClass = DocDataProviderConfig.Turns::class

    override suspend fun provideData(
        config: DocDataProviderConfig.Turns,
        fullGameData: FullGameData,
        statProcessorData: StatProcessorData,
        provider: AdditionalDataProvider
    ) = fullGameData.getActiveTurns(statProcessorData, provider)
}
