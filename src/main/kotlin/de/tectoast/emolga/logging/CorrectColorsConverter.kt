package de.tectoast.emolga.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.pattern.color.ANSIConstants
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase

class CorrectColorsConverter : ForegroundCompositeConverterBase<ILoggingEvent>() {
    override fun getForegroundColorCode(e: ILoggingEvent): String {
        return if (hasMarker(e = e)) ANSIConstants.CYAN_FG else when (e.level.toInt()) {
            Level.ERROR_INT -> ANSIConstants.BOLD + ANSIConstants.RED_FG
            Level.WARN_INT -> ANSIConstants.YELLOW_FG
            else -> ANSIConstants.DEFAULT_FG
        }
    }

    companion object {
        private fun hasMarker(marker: String = "important", e: ILoggingEvent): Boolean {
            return e.markerList != null && e.markerList.any { it.name == marker }
        }
    }
}