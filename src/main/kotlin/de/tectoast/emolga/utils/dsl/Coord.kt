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

    override fun toString() = "'${sheet.replace("'", "''")}'!${convert(x)}$y"

    companion object {
        fun convert(x: Int) = intToStringCache.getOrPut(x) { x.xc() }
        fun convert(x: String) = stringToIntCache.getOrPut(x) { getColumnFromRange(x) + 1 }
        private val intToStringCache = mutableMapOf<Int, String>()
        private val stringToIntCache = mutableMapOf<String, Int>()
    }
}

