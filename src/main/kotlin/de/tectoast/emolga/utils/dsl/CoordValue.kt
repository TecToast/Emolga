package de.tectoast.emolga.utils.dsl

import kotlinx.serialization.Serializable

@Serializable
data class CoordValue(val coord: CoordExpr, val value: SheetValueExpr)