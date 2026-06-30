package de.tectoast.emolga.domain.league.doc.service.provider.data

import de.tectoast.emolga.domain.league.doc.model.AdditionalDataProvider
import de.tectoast.emolga.domain.league.doc.model.DocDataProviderConfig
import de.tectoast.emolga.domain.league.doc.model.StatProcessorData
import de.tectoast.emolga.domain.league.gamedata.model.FullGameData
import org.koin.core.annotation.Single

@Single
class DamageDirectDocDataProviderHandler : DocDataProviderHandler<DocDataProviderConfig.DamageDirect> {
    override val targetClass = DocDataProviderConfig.DamageDirect::class

    override suspend fun provideData(
        config: DocDataProviderConfig.DamageDirect,
        fullGameData: FullGameData,
        statProcessorData: StatProcessorData,
        provider: AdditionalDataProvider
    ) = fullGameData.getDamageDealt(statProcessorData, provider, active = true)
}