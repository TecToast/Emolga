package de.tectoast.emolga.commands.flo;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.sql.DBManagers;
import net.dv8tion.jda.api.JDA;

import java.util.concurrent.Executors;

import static de.tectoast.emolga.utils.Constants.CALENDAR_MSGID;
import static de.tectoast.emolga.utils.Constants.CALENDAR_TCID;

public class UpdateRemindersCommand extends Command {

    public UpdateRemindersCommand() {
        super("updatereminders", "Updated die Reminder lol", CommandCategory.Flo);
    }

    @Override
    public void process(GuildCommandEvent e) {
        calendarService.shutdownNow();
        calendarService = Executors.newScheduledThreadPool(10);
        JDA jda = e.getJDA();
        jda.getTextChannelById(CALENDAR_TCID).editMessageById(CALENDAR_MSGID, buildCalendar()).queue();
        DBManagers.CALENDAR.getAllEntries().forEach(Command::scheduleCalendarEntry);
        e.done();
    }
}
