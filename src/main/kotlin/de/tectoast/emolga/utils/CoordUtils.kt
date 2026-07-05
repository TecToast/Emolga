package de.tectoast.emolga.utils

import de.tectoast.emolga.utils.dsl.Coord
import kotlin.math.pow

infix fun String.x(x: Int) = Coord(this, x, 0)
infix fun String.x(x: String) = Coord(this, x, 0)
infix fun Coord.y(y: Int) = Coord(sheet, x, y)


fun Int.coordXMod(sheet: String, num: Int, xFactor: Int, xSummand: Int, yFactor: Int = 1, ySummand: Int) =
    Coord(sheet, this % num * xFactor + xSummand, this / num * yFactor + ySummand)

fun Int.coordXModShift(
    sheet: String,
    num: Int,
    xFactor: Int,
    xSummand: Int,
    yFactor: Int = 1,
    ySummand: Int,
    shifts: Map<Int, Int>
): Coord {
    val baseToUse = shifts[this] ?: this
    return baseToUse.coordXMod(sheet, num, xFactor, xSummand, yFactor, ySummand)
}


/**
 * Converts an integer to a column name in Google Sheets.
 *
 * Examples: `1 -> A, 3 -> C, 27 -> AA, 28 -> AB, 53 -> BA`
 * @param xc the column number
 * @return the column name
 */
fun getAsXCoord(xc: Int): String {
    var x = 0
    var toResolve = xc
    while (true) {
        val powed = 26.0.pow(x).toInt()
        if (toResolve < powed) break
        toResolve -= powed
        x++
    }
    return buildString {
        repeat(x) {
            append(((toResolve % 26) + 65).toChar())
            toResolve /= 26
        }
        reverse()
    }
}

fun Int.x(factor: Int, summand: Int) = getAsXCoord(y(factor, summand))

fun Int.xc() = getAsXCoord(this)

fun Int.y(factor: Int, summand: Int) = this * factor + summand