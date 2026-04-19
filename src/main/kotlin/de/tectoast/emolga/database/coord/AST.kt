package de.tectoast.emolga.database.coord

import de.tectoast.emolga.database.league.PickVariable
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.records.CoordXMod
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

interface AstEnvironment {
    fun <T : Any> resolve(variable: Enum<*>, clazz: KClass<T>): T
}
fun AstEnvironment.resolveInt(variable: Enum<*>): Int = resolve(variable, Int::class)
fun AstEnvironment.resolveString(variable: Enum<*>): String = resolve(variable, String::class)
fun AstEnvironment.resolveBoolean(variable: Enum<*>): Boolean = resolve(variable, Boolean::class)

@Serializable
sealed interface SheetValueExpr {
    fun evalForSheet(env: AstEnvironment): Any
}

@Serializable
data class CoordValue(val coord: CoordExpr, val value: SheetValueExpr)

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
sealed interface CoordExpr {
    fun eval(env: AstEnvironment): Coord
}

@Serializable
@SerialName("StringLiteral")
data class LiteralString(val value: String) : StringExpr, SheetValueExpr {
    override fun eval(env: AstEnvironment) = value
}

@Serializable
@SerialName("CoordLiteral")
data class LiteralCoord(val sheet: String, val x: Int, val y: Int) : CoordExpr {
    override fun eval(env: AstEnvironment) = Coord(sheet, x, y)
}

@Serializable
@SerialName("CoordXMod")
data class CoordXMod(
    val base: IntExpr,
    val sheet: String,
    val num: Int,
    val xFactor: Int,
    val xSummand: Int,
    val yFactor: Int,
    val ySummand: Int
) : CoordExpr {
    override fun eval(env: AstEnvironment): Coord {
        val i = base.eval(env)
        return i.CoordXMod(sheet, num, xFactor, xSummand, yFactor, ySummand)
    }
}

@Serializable
@SerialName("IfElse")
data class IfEqualsElseCoord(
    val variable: IntExpr,
    val condition: IntExpr,
    val ifEqual: CoordExpr,
    val otherwise: CoordExpr
) : CoordExpr {
    override fun eval(env: AstEnvironment): Coord {
        return if (variable.eval(env) == condition.eval(env)) ifEqual.eval(env) else otherwise.eval(env)
    }
}


@Serializable
@SerialName("Literal")
data class LiteralInt(val value: Int) : IntExpr, SheetValueExpr {
    override fun eval(env: AstEnvironment): Int = value
}

@Serializable
@SerialName("Variable")
data class PickVariableInt(val variable: PickVariable) : IntExpr, SheetValueExpr {
    override fun eval(env: AstEnvironment) = env.resolveInt(variable)
}

@Serializable
@SerialName("Add")
data class AddInt(val left: IntExpr, val right: IntExpr) : IntExpr, SheetValueExpr {
    override fun eval(env: AstEnvironment) = left.eval(env) + right.eval(env)
}

@Serializable
@SerialName("MultAdd")
data class MultAddInt(val base: IntExpr, val factor: IntExpr, val summand: IntExpr) : IntExpr, SheetValueExpr {
    override fun eval(env: AstEnvironment) = base.eval(env) * factor.eval(env) + summand.eval(env)
}

@Serializable
@SerialName("IfEqualsElse")
data class IfEqualsElseInt(
    val variable: IntExpr,
    val condition: IntExpr,
    val ifEqual: IntExpr,
    val otherwise: IntExpr
) : IntExpr, SheetValueExpr {
    override fun eval(env: AstEnvironment): Int {
        return if (variable.eval(env) == condition.eval(env)) ifEqual.eval(env) else otherwise.eval(env)
    }
}