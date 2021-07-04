package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.IntColumn;
import de.tectoast.emolga.utils.sql.base.columns.LongColumn;

public class BirthdayManager extends DataManager {

    final LongColumn USERID = new LongColumn("userid", this);
    final IntColumn YEAR = new IntColumn("year", this);
    final IntColumn MONTH = new IntColumn("month", this);
    final IntColumn DAY = new IntColumn("day", this);
    

    public BirthdayManager() {
        super("birthdays");
        setColumns(USERID, YEAR, MONTH, DAY);
    }

    public void addOrUpdateBirthday(long userid, int year, int month, int day) {
        /*readWrite(select(USERID.check(userid)), set -> {
            if (set.next()) {
                YEAR.update(set, year);
                MONTH.update(set, month);
                DAY.update(set, day);
                set.updateRow();
            } else {
                set.moveToInsertRow();
                USERID.update(set, userid);
                YEAR.update(set, year);
                MONTH.update(set, month);
                DAY.update(set, day);
                set.insertRow();
            }
        });*/
        insertOrUpdate(USERID, userid, userid, year, month, day);
    }
}
