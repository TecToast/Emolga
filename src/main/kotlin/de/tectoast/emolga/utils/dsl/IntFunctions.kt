package de.tectoast.emolga.utils.dsl

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("IntL")
data class IntLiteral(private val value: Int) : IntExpr, SheetValueExpr {
    override fun eval(env: AstEnvironment): Int = value
}

@Serializable
@SerialName("IntV")
data class IntVariable(private val variable: String) : IntExpr, SheetValueExpr {
    override fun eval(env: AstEnvironment) = env.resolveInt(variable)
}

@Serializable
@SerialName("IntAdd")
data class IntAdd(private val left: IntExpr, private val right: IntExpr) : IntExpr, SheetValueExpr {
    override fun eval(env: AstEnvironment) = left.eval(env) + right.eval(env)
}

@Serializable
@SerialName("IntMultAdd")
data class IntMultAdd(private val base: IntExpr, private val factor: IntExpr, private val summand: IntExpr) : IntExpr,
    SheetValueExpr {
    override fun eval(env: AstEnvironment) = base.eval(env) * factor.eval(env) + summand.eval(env)
}

@Serializable
@SerialName("IntIfElse")
data class IntIfElse(
    private val condition: BooleanExpr,
    private val ifTrue: IntExpr,
    private val ifFalse: IntExpr
) : IntExpr, SheetValueExpr {
    override fun eval(env: AstEnvironment): Int {
        return if (condition.eval(env)) ifTrue.eval(env) else ifFalse.eval(env)
    }
}

@Serializable
@SerialName("IntEquals")
data class IntEquals(private val a: IntExpr, private val b: IntExpr) : BooleanExpr, SheetValueExpr {
    override fun eval(env: AstEnvironment): Boolean = a.eval(env) == b.eval(env)
}
