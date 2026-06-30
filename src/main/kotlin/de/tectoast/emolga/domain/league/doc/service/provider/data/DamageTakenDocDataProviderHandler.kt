package de.tectoast.emolga.domain.league.doc.service.provider.data

import de.tectoast.emolga.domain.league.doc.model.AdditionalDataProvider
import de.tectoast.emolga.domain.league.doc.model.DocDataProviderConfig
import de.tectoast.emolga.domain.league.doc.model.StatProcessorData
import de.tectoast.emolga.domain.league.gamedata.model.FullGameData
import org.koin.core.annotation.Single

@Single
class DamageTakenDocDataProviderHandler : DocDataProviderHandler<DocDataProviderConfig.DamageTaken> {
    override val targetClass = DocDataProviderConfig.DamageTaken::class

    override suspend fun provideData(
        config: DocDataProviderConfig.DamageTaken,
        fullGameData: FullGameData,
        statProcessorData: StatProcessorData,
        provider: AdditionalDataProvider
    ) = fullGameData.getDamageTaken(statProcessorData, provider)
}