package de.tectoast.emolga.commands.flegmon;

import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.commands.PepeCommand;
import de.tectoast.emolga.database.Database;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

import java.sql.SQLException;

public class SetBirthdayCommand extends PepeCommand {
    public SetBirthdayCommand() {
        super("setbirthday", "Trägt deinen Geburtstag ein");
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("bday", "Geburtstag", "Der Geburtstag lol", ArgumentManagerTemplate.Text.any())
                .setExample("!setbirthday 30.01.2005").build());
    }

    @Override
    public void process(GuildCommandEvent e) throws SQLException {
        String msg = e.getMessage().getContentDisplay();
        Member member = e.getMember();
        TextChannel tco = e.getChannel();
        String birthday = msg.split(" ")[1];
        String[] bd = birthday.split("\\.");
        if (bd.length != 3) {
            e.reply("Das ist kein valides Datum!");
            return;
        }
        int day;
        int month;
        int year;
        try {
            day = Integer.parseInt(bd[0]);
        } catch (NumberFormatException ex) {
            e.reply("Das ist kein valider Tag!");
            return;
        }
        try {
            month = Integer.parseInt(bd[1]);
        } catch (NumberFormatException ex) {
            e.reply("Das ist kein valider Monat!");
            return;
        }
        try {
            year = Integer.parseInt(bd[2]);
        } catch (NumberFormatException ex) {
            e.reply("Das ist kein valides Jahr!");
            return;
        }
        long uid = e.getAuthor().getIdLong();
        if (Database.update("UPDATE birthdays SET year = " + year + ", month = " + month + ", day = " + day + " WHERE userid = " + uid) == 0) {
            Database.insert("birthdays", "userid, year, month, day", uid, year, month, day);
        }
        tco.sendMessage("Dein Geburtstag wurde erfolgreich auf den " + getWithZeros(day, 2) + "." + getWithZeros(month, 2) + "." + year + " gesetzt!").queue();
    }
}
