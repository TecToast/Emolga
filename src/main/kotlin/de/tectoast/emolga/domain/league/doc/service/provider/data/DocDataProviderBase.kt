package de.tectoast.emolga.domain.league.doc.service.provider.data

import de.tectoast.emolga.domain.league.doc.model.AdditionalDataProvider
import de.tectoast.emolga.domain.league.doc.model.DocDataProviderConfig
import de.tectoast.emolga.domain.league.doc.model.StatProcessorData
import de.tectoast.emolga.domain.league.gamedata.model.FullGameData
import de.tectoast.emolga.utils.handler.BaseHandler

interface DocDataProviderOperations<C : DocDataProviderConfig> {
    suspend fun provideData(
        config: C,
        fullGameData: FullGameData,
        statProcessorData: StatProcessorData,
        provider: AdditionalDataProvider
    ): Any
}

interface DocDataProviderHandler<C : DocDataProviderConfig> : BaseHandler<C>, DocDataProviderOperations<C>
