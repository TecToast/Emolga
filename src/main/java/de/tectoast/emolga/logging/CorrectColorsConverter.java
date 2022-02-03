package de.tectoast.emolga.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase;

import static ch.qos.logback.core.pattern.color.ANSIConstants.*;

@SuppressWarnings("SameParameterValue")
public class CorrectColorsConverter extends ForegroundCompositeConverterBase<ILoggingEvent> {
    @Override
    protected String getForegroundColorCode(ILoggingEvent e) {
        if (hasMarker("important", e)) return CYAN_FG;
        return switch (e.getLevel().toInt()) {
            case Level.ERROR_INT -> BOLD + RED_FG;
            case Level.WARN_INT -> YELLOW_FG;
            default -> DEFAULT_FG;
        };
    }

    private boolean hasMarker(String marker, ILoggingEvent e) {
        return e.getMarkerList() != null && e.getMarkerList().stream().anyMatch(m -> m.getName().equals(marker));
    }
}
