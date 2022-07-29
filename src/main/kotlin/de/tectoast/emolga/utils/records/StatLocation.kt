package de.tectoast.emolga.utils.records

import de.tectoast.emolga.commands.Command.Companion.getAsXCoord

//String sheet, String x, int y
class StatLocation(private val sheet: String?, val x: String, private val y: Int) {
    constructor(sheet: String?, x: Int, y: Int) : this(sheet, getAsXCoord(x), y)

    val isValid: Boolean
        get() = sheet != null

    override fun toString(): String {
        return "$sheet!$x$y"
    }

    companion object {
        private val INVALD = StatLocation(null, 0, 0)
        fun invalid(): StatLocation {
            return INVALD
        }
    }
}