package de.tectoast.emolga.domain.league.doc.service.provider.data

import de.tectoast.emolga.domain.league.doc.model.AdditionalDataProvider
import de.tectoast.emolga.domain.league.doc.model.DocDataProviderConfig
import de.tectoast.emolga.domain.league.doc.model.StatProcessorData
import de.tectoast.emolga.domain.league.gamedata.model.FullGameData
import org.koin.core.annotation.Single

@Single
class DiffDocDataProviderHandler : DocDataProviderHandler<DocDataProviderConfig.Diff> {
    override val targetClass = DocDataProviderConfig.Diff::class

    override suspend fun provideData(
        config: DocDataProviderConfig.Diff,
        fullGameData: FullGameData,
        statProcessorData: StatProcessorData,
        provider: AdditionalDataProvider
    ): Int {
        val kd = fullGameData.getKDWithName(statProcessorData)
        return kd.kills - kd.deaths
    }
}