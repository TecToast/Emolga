package de.tectoast.emolga.utils.records;

import java.sql.Timestamp;

public record CalendarEntry(String message, Timestamp expires) {
}
