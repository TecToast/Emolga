package de.tectoast.emolga.utils.dsl

import kotlinx.serialization.Serializable

@Serializable
sealed interface SheetValueExpr {
    fun evalForSheet(env: AstEnvironment): Any
}