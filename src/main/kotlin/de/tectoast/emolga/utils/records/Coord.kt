@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package de.tectoast.emolga.utils.records

import de.tectoast.emolga.commands.xc
import de.tectoast.emolga.utils.RequestBuilder


data class Coord(val sheet: String, val x: Int, val y: Int) {
    constructor(sheet: String, x: String, y: Int) : this(sheet, RequestBuilder.getColumnFromRange(x) + 1, y)

    override fun toString() = "'$sheet'!$xAsC$y"

    val withoutSheet get() = "$xAsC$y"

    val xAsC: String get() = x.xc()

    fun plusX(x: Int) = Coord(sheet, this.x + x, y)
    fun plusY(y: Int) = Coord(sheet, x, this.y + y)
    fun setX(x: Int) = Coord(sheet, x, y)
    fun setX(x: String) = Coord(sheet, RequestBuilder.getColumnFromRange(x) + 1, y)
    fun setY(y: Int) = Coord(sheet, x, y)
    operator fun plus(xy: Pair<Int, Int>) = Coord(sheet, x + xy.first, y + xy.second)

    fun spread(x: Int = 0, y: Int = 0) = "$this:${(this + (x to y)).withoutSheet}"
}

@Suppress("FunctionName")
fun Int.CoordXMod(sheet: String, num: Int, xFactor: Int, xSummand: Int, yFactor: Int = 1, ySummand: Int) =
    Coord(sheet, this % num * xFactor + xSummand, this / num * yFactor + ySummand)

infix fun String.x(x: Int) = Coord(this, x, 0)
infix fun String.x(x: String) = Coord(this, RequestBuilder.getColumnFromRange(x) + 1, 0)
infix fun Coord.y(y: Int) = Coord(sheet, x, y)

infix fun String.xy(xy: Pair<Int, Int>) = Coord(this, xy.first, xy.second)
fun String.toCoord() = this.split("!").let {
    val sheet = it[0].removeSurrounding("'")
    val x = it[1].replace(Regex("[^A-Z]"), "")
    val y = it[1].replace(Regex("[^0-9]"), "").toInt()
    Coord(sheet, x, y)
}
