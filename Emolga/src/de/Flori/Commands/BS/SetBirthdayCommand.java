package de.Flori.Commands.BS;

import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONObject;

public class SetBirthdayCommand extends Command {
    public SetBirthdayCommand() {
        super("setbirthday", "`!setbirthday <Geburtsdatum>` Trägt deinen Geburtstag ein (z.B. `30.01.2005`)", CommandCategory.BS);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        String msg = e.getMessage().getContentDisplay();
        Member member = e.getMember();
        TextChannel tco = e.getChannel();
        String birthday = msg.split(" ")[1];
        String[] bd = birthday.split("\\.");
        int day = Integer.parseInt(bd[0]);
        int month = Integer.parseInt(bd[1]);
        int year = Integer.parseInt(bd[2]);
        JSONObject json = getEmolgaJSON();
        if(!json.has("birthdays")) json.put("birthdays", new JSONObject());
        JSONObject obj = new JSONObject();
        obj.put("day", day);
        obj.put("month", month);
        obj.put("year", year);
        json.getJSONObject("birthdays").put(member.getId(), obj);
        tco.sendMessage("Dein Geburtstag wurde erfolgreich auf den " + getWithZeros(day, 2) + "." + getWithZeros(month, 2) + "." + year + " gesetzt!").queue();
        saveEmolgaJSON();
    }
}
