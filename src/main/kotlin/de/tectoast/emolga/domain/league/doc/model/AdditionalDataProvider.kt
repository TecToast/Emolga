package de.tectoast.emolga.domain.league.doc.model

import de.tectoast.emolga.domain.league.doc.service.provider.event.AnalysisEventProvider
import de.tectoast.emolga.domain.league.doc.service.provider.monname.MonNameProvider

data class AdditionalDataProvider(
    val monNameProvider: MonNameProvider,
    val analysisEventProvider: AnalysisEventProvider,
)