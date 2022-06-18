package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.IntColumn;
import de.tectoast.emolga.utils.sql.base.columns.LongColumn;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.util.Calendar;
import java.util.List;

import static de.tectoast.emolga.utils.sql.base.Condition.and;

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
        //insertOrUpdate(USERID, userid, userid, year, month, day);
        replaceIfExists(userid, year, month, day);
    }

    public void checkBirthdays(Calendar c, MessageChannel tc) {
        read(selectAll(and(MONTH.check(c.get(Calendar.MONTH) + 1), DAY.check(c.get(Calendar.DAY_OF_MONTH)))), s -> {
            forEach(s, set -> tc.sendMessage("Alles Gute zum " + (Calendar.getInstance().get(Calendar.YEAR) - YEAR.getValue(set)) + ". Geburtstag, <@" + USERID.getValue(set) + ">!").queue());
        });
    }

    public List<Data> getAll() {
        return read(selectAll(), set -> {
            return map(set, s -> new Data(USERID.getValue(s), MONTH.getValue(s), DAY.getValue(s)));
        });
    }

    public record Data(long userid, int month, int day) {
    }
}
