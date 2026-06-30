package de.tectoast.emolga.utils.dsl

import de.tectoast.emolga.utils.sheetupdate.getColumnFromRange
import de.tectoast.emolga.utils.xc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Coord")
data class Coord(val sheet: String, val x: Int, val y: Int) {

    constructor(sheet: String, x: String, y: Int) : this(
        sheet,
        convert(x), y
    )

    constructor(sheet: String, xy: String) : this(
        sheet,
        convert(xy.replace(Regex("[^A-Z]"), "")),
        xy.replace(Regex("[^0-9]"), "").toInt()
    )

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
        fun convert(x: String) = stringToIntCache.getOrPut(x) { getColumnFromRange(x) + 1 }
        private val intToStringCache = mutableMapOf<Int, String>()
        private val stringToIntCache = mutableMapOf<String, Int>()
    }
}

