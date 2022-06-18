package de.tectoast.emolga.commands.flo;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.Constants;
import de.tectoast.emolga.utils.sql.DBManagers;
import net.dv8tion.jda.api.entities.TextChannel;

import java.sql.Timestamp;

import static de.tectoast.emolga.utils.Constants.CALENDAR_MSGID;
import static de.tectoast.emolga.utils.Constants.CALENDAR_TCID;

public class RemindCommand extends Command {

    public RemindCommand() {
        super("remind", "Setzt einen Reminder auf", CommandCategory.Flo, Constants.MYSERVER);
    }

    @Override
    public void process(GuildCommandEvent e) {
        try {
            String[] split = WHITESPACES_SPLITTER.split(e.getMessage().getContentRaw(), 3);
            long expires = parseCalendarTime(split[1]);
            String message = split[2];
            TextChannel calendarTc = e.getJDA().getTextChannelById(CALENDAR_TCID);
            DBManagers.CALENDAR.insertNewEntry(message, new Timestamp(expires / 1000 * 1000));
            scheduleCalendarEntry(expires, message);
            e.getMessage().delete().queue();
            calendarTc.editMessageById(CALENDAR_MSGID, buildCalendar()).queue();
        } catch (NumberFormatException ex) {
            e.getChannel().sendMessage("Das ist keine valide Zeitangabe!").queue();
            ex.printStackTrace();
        } catch (Exception ex) {
            e.getChannel().sendMessage("Es ist ein Fehler aufgetreten!").queue();
            ex.printStackTrace();
        }
    }


}
