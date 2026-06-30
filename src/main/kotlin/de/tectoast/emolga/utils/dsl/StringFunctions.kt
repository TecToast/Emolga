package de.tectoast.emolga.utils.dsl

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("StringL")
data class StringLiteral(private val value: String) : StringExpr, SheetValueExpr {
    override fun eval(env: AstEnvironment) = value
}

@Serializable
@SerialName("StringV")
data class StringVariable(private val variable: String) : StringExpr, SheetValueExpr {
    override fun eval(env: AstEnvironment) = env.resolveString(variable)
}

@Serializable
@SerialName("StringList")
data class StringFromList(private val list: List<String>, private val index: IntExpr) : StringExpr, SheetValueExpr {
    override fun eval(env: AstEnvironment): String {
        val idx = index.eval(env)
        if (idx < 0 || idx >= list.size) throw IllegalArgumentException("Index $idx out of bounds for list of size ${list.size}")
        return list[idx]
    }
}