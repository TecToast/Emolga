package de.tectoast.emolga.league

import de.tectoast.emolga.commands.y
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.records.CoordXMod
import kotlinx.serialization.Serializable

@Serializable
sealed class DynamicCoord {
    abstract operator fun invoke(i: Int): Coord

    @Serializable
    data class HardCoded(val coords: Map<Int, Coord>) : DynamicCoord() {
        override fun invoke(i: Int) = coords[i] ?: throw CouldNotCalculateException()
    }

    @Serializable
    data class ModuloX(
        private val sheet: String,
        val num: Int,
        private val xFactor: Int,
        private val xSummand: Int,
        private val yFactor: Int,
        private val ySummand: Int
    ) : DynamicCoord() {
        override operator fun invoke(i: Int) = i.CoordXMod(sheet, num, xFactor, xSummand, yFactor, ySummand)
    }

    @Serializable
    data class X(
        private val sheet: String,
        private val xFactor: Int,
        private val xSummand: Int,
        private val ySummand: Int
    ) : DynamicCoord() {
        override operator fun invoke(i: Int) = Coord(sheet, i.y(xFactor, xSummand), ySummand)
    }

    @Serializable
    data class Y(
        private val sheet: String,
        private val xSummand: Int,
        private val yFactor: Int,
        private val ySummand: Int
    ) : DynamicCoord() {
        override operator fun invoke(i: Int) = Coord(sheet, xSummand, i.y(yFactor, ySummand))
    }

}

class CouldNotCalculateException : Exception()
