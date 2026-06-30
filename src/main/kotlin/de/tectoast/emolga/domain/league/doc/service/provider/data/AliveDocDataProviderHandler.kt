package de.tectoast.emolga.domain.league.doc.service.provider.data

import de.tectoast.emolga.domain.league.doc.model.AdditionalDataProvider
import de.tectoast.emolga.domain.league.doc.model.DocDataProviderConfig
import de.tectoast.emolga.domain.league.doc.model.StatProcessorData
import de.tectoast.emolga.domain.league.gamedata.model.FullGameData
import org.koin.core.annotation.Single

@Single
class AliveDocDataProviderHandler : DocDataProviderHandler<DocDataProviderConfig.Alive> {
    override val targetClass = DocDataProviderConfig.Alive::class

    override suspend fun provideData(
        config: DocDataProviderConfig.Alive,
        fullGameData: FullGameData,
        statProcessorData: StatProcessorData,
        provider: AdditionalDataProvider
    ) = 1 - fullGameData.getKDWithName(statProcessorData).deaths
}