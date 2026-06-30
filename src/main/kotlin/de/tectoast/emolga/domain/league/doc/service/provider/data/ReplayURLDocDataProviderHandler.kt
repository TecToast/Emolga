package de.tectoast.emolga.domain.league.doc.service.provider.data

import de.tectoast.emolga.domain.league.doc.model.AdditionalDataProvider
import de.tectoast.emolga.domain.league.doc.model.DocDataProviderConfig
import de.tectoast.emolga.domain.league.doc.model.StatProcessorData
import de.tectoast.emolga.domain.league.gamedata.model.FullGameData
import org.koin.core.annotation.Single

@Single
class ReplayURLDocDataProviderHandler : DocDataProviderHandler<DocDataProviderConfig.ReplayURL> {
    override val targetClass = DocDataProviderConfig.ReplayURL::class

    override suspend fun provideData(
        config: DocDataProviderConfig.ReplayURL,
        fullGameData: FullGameData,
        statProcessorData: StatProcessorData,
        provider: AdditionalDataProvider
    ): Any {
        val url = fullGameData.games[statProcessorData.matchNum].url
        return config.text?.let { "=HYPERLINK(\"$url\", \"$it\")" } ?: url
    }
}