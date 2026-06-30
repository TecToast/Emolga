package de.tectoast.emolga.domain.league.doc.model

import de.tectoast.emolga.utils.dsl.CoordExpr
import kotlinx.serialization.Serializable

@Serializable
data class StatProcessor(val coord: CoordExpr, val provider: DocDataProviderConfig)