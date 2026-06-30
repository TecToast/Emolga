package de.tectoast.emolga.domain.league.doc.service.provider.data

import de.tectoast.emolga.domain.league.doc.model.AdditionalDataProvider
import de.tectoast.emolga.domain.league.doc.model.DocDataProviderConfig
import de.tectoast.emolga.domain.league.doc.model.StatProcessorData
import de.tectoast.emolga.domain.league.gamedata.model.FullGameData
import org.koin.core.annotation.Single

@Single
class DamageIndirectDocDataProviderHandler : DocDataProviderHandler<DocDataProviderConfig.DamageIndirect> {
    override val targetClass = DocDataProviderConfig.DamageIndirect::class

    override suspend fun provideData(
        config: DocDataProviderConfig.DamageIndirect,
        fullGameData: FullGameData,
        statProcessorData: StatProcessorData,
        provider: AdditionalDataProvider
    ) = fullGameData.getDamageDealt(statProcessorData, provider, active = false)
}