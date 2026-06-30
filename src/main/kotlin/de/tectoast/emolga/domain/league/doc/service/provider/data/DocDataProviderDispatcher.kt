package de.tectoast.emolga.domain.league.doc.service.provider.data

import de.tectoast.emolga.domain.league.doc.model.AdditionalDataProvider
import de.tectoast.emolga.domain.league.doc.model.DocDataProviderConfig
import de.tectoast.emolga.domain.league.doc.model.StatProcessorData
import de.tectoast.emolga.domain.league.gamedata.model.FullGameData
import de.tectoast.emolga.utils.handler.HandlerRegistry
import org.koin.core.annotation.Single

@Single
class DocDataProviderDispatcher(handlers: List<DocDataProviderHandler<DocDataProviderConfig>>) :
    DocDataProviderOperations<DocDataProviderConfig> {
    private val registry = HandlerRegistry(handlers)

    override suspend fun provideData(
        config: DocDataProviderConfig,
        fullGameData: FullGameData,
        statProcessorData: StatProcessorData,
        provider: AdditionalDataProvider
    ) = registry.getHandler(config).provideData(config, fullGameData, statProcessorData, provider)
}