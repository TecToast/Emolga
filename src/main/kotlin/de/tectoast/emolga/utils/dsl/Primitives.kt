package de.tectoast.emolga.utils.dsl

import kotlinx.serialization.Serializable

@Serializable
sealed interface IntExpr : SheetValueExpr {
    fun eval(env: AstEnvironment): Int
    override fun evalForSheet(env: AstEnvironment) = eval(env)
}

@Serializable
sealed interface StringExpr : SheetValueExpr {
    fun eval(env: AstEnvironment): String
    override fun evalForSheet(env: AstEnvironment) = eval(env)
}

@Serializable
sealed interface BooleanExpr : SheetValueExpr {
    fun eval(env: AstEnvironment): Boolean
    override fun evalForSheet(env: AstEnvironment) = eval(env)
}

@Serializable
sealed interface CoordExpr {
    fun eval(env: AstEnvironment): Coord
}