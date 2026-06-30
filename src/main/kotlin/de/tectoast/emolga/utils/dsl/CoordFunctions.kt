package de.tectoast.emolga.utils.dsl

import de.tectoast.emolga.utils.coordXMod
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("CoordL")
data class CoordLiteral(private val sheet: StringExpr, private val x: IntExpr, private val y: IntExpr) : CoordExpr {
    override fun eval(env: AstEnvironment) = Coord(sheet.eval(env), x.eval(env), y.eval(env))
}


@Serializable
@SerialName("CoordXMod")
data class CoordXMod(
    private val base: IntExpr,
    private val sheet: StringExpr,
    private val num: IntExpr,
    private val xFactor: IntExpr,
    private val xSummand: IntExpr,
    private val yFactor: IntExpr,
    private val ySummand: IntExpr
) : CoordExpr {
    override fun eval(env: AstEnvironment): Coord {
        val i = base.eval(env)
        return i.coordXMod(
            sheet.eval(env),
            num.eval(env),
            xFactor.eval(env),
            xSummand.eval(env),
            yFactor.eval(env),
            ySummand.eval(env)
        )
    }
}

@Serializable
@SerialName("CoordIfElse")
data class CoordIfElse(
    private val condition: BooleanExpr,
    private val ifTrue: CoordExpr,
    private val ifFalse: CoordExpr
) : CoordExpr {
    override fun eval(env: AstEnvironment): Coord {
        return if (condition.eval(env)) ifTrue.eval(env) else ifFalse.eval(env)
    }
}
