package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.utils.records.CalendarEntry;
import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.StringColumn;
import de.tectoast.emolga.utils.sql.base.columns.TimestampColumn;

import java.sql.Timestamp;
import java.util.List;

public class CalendarManager extends DataManager {

    final StringColumn MESSAGE = new StringColumn("message", this);
    final TimestampColumn EXPIRES = new TimestampColumn("expires", this);

    public CalendarManager() {
        super("calendar");
        setColumns(MESSAGE, EXPIRES);
    }

    public void insertNewEntry(String message, Timestamp expires) {
        insert(message, expires);
    }

    public List<CalendarEntry> getAllEntries() {
        return read(selectAll(), s -> {
            return map(s, set -> new CalendarEntry(MESSAGE.getValue(set), EXPIRES.getValue(set)));
        });
    }

    public void delete(Timestamp expires) {
        delete(EXPIRES.check(expires));
    }
}
