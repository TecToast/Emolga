package de.tectoast.emolga.domain.league.doc.service.provider.data

import de.tectoast.emolga.domain.league.doc.model.AdditionalDataProvider
import de.tectoast.emolga.domain.league.doc.model.DocDataProviderConfig
import de.tectoast.emolga.domain.league.doc.model.StatProcessorData
import de.tectoast.emolga.domain.league.gamedata.model.FullGameData
import org.koin.core.annotation.Single

@Single
class PokemonDocDataProviderHandler : DocDataProviderHandler<DocDataProviderConfig.Pokemon> {
    override val targetClass = DocDataProviderConfig.Pokemon::class

    override suspend fun provideData(
        config: DocDataProviderConfig.Pokemon,
        fullGameData: FullGameData,
        statProcessorData: StatProcessorData,
        provider: AdditionalDataProvider
    ) = provider.monNameProvider.getDisplayName(
        fullGameData.getKDWithName(statProcessorData).name
    )
}