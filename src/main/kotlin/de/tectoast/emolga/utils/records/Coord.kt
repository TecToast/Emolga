package de.tectoast.emolga.utils.records

import de.tectoast.emolga.commands.xc
import de.tectoast.emolga.league.LeagueCreator
import de.tectoast.emolga.utils.RequestBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Coord")
data class Coord(val sheet: String, val x: Int, val y: Int) : TableCoord {

    constructor(sheet: String, x: String, y: Int) : this(
        sheet,
        convert(x), y
    )

    override fun provideCoord(index: Int, lc: LeagueCreator): Coord {
        return plusY(index * lc.tableStep)
    }

    override fun toString() = "'${sheet.replace("'", "''")}'!$xAsC$y"
    val formula get() = "=$this"

    val withoutSheet get() = "$xAsC$y"

    val xAsC: String get() = convert(x)

    fun plusX(x: Int) = Coord(sheet, this.x + x, y)
    fun plusY(y: Int) = Coord(sheet, x, this.y + y)
    fun setX(x: Int) = Coord(sheet, x, y)
    fun setX(x: String) =
        Coord(sheet, convert(x), y)
    fun setY(y: Int) = Coord(sheet, x, y)
    operator fun plus(xy: Pair<Int, Int>) = Coord(sheet, x + xy.first, y + xy.second)

    fun spreadBy(x: Int = 0, y: Int = 0) = "$this:${(this + (x to y)).withoutSheet}"
    fun spreadTo(x: Int = 1, y: Int = 1) = "$this:${(this + ((x - 1) to (y - 1))).withoutSheet}"

    companion object {
        fun convert(x: Int) = intToStringCache.getOrPut(x) { x.xc() }
        fun convert(x: String) = stringToIntCache.getOrPut(x) { RequestBuilder.getColumnFromRange(x) + 1 }
        private val intToStringCache = mutableMapOf<Int, String>()
        private val stringToIntCache = mutableMapOf<String, Int>()
    }
}

fun String.toCoord() = this.replace("=", "").split("!").let {
    val sheet = it[0].removeSurrounding("'")
    val x = it[1].replace(Regex("[^A-Z]"), "")
    val y = it[1].replace(Regex("[^0-9]"), "").toInt()
    Coord(sheet, x, y)
}


@Suppress("FunctionName")
fun Int.CoordXMod(sheet: String, num: Int, xFactor: Int, xSummand: Int, yFactor: Int = 1, ySummand: Int) =
    Coord(sheet, this % num * xFactor + xSummand, this / num * yFactor + ySummand)

infix fun String.x(x: Int) = Coord(this, x, 0)
infix fun String.x(x: String) = Coord(this, x, 0)
infix fun Coord.y(y: Int) = Coord(sheet, x, y)

infix fun String.xy(xy: Pair<Int, Int>) = Coord(this, xy.first, xy.second)
@Serializable
sealed interface TableCoord {
    fun provideCoord(index: Int, lc: LeagueCreator): Coord
}
@Serializable
@SerialName("IndexAwareTableCoord")
data class IndexAwareTableCoord(val mapping: Map<List<Int>, Coord>) :
    TableCoord {
        constructor(vararg mappings: Pair<List<Int>, Coord>): this(mappings.toMap())
    override fun provideCoord(index: Int, lc: LeagueCreator): Coord {
        val (indices, coord) = mapping.entries.toList().first { index in it.key }
        return coord.plusY(indices.indexOf(index) * lc.tableStep)
    }
}
