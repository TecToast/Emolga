package de.tectoast.emolga.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase;

import static ch.qos.logback.core.pattern.color.ANSIConstants.*;

public class CorrectColorsConverter extends ForegroundCompositeConverterBase<ILoggingEvent> {
    @Override
    protected String getForegroundColorCode(ILoggingEvent e) {
        return switch (e.getLevel().toInt()) {
            case Level.ERROR_INT -> BOLD + RED_FG;
            case Level.WARN_INT -> YELLOW_FG;
            default -> DEFAULT_FG;
        };
    }
}
