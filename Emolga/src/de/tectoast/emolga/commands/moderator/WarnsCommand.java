package de.tectoast.emolga.commands.moderator;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.database.Database;
import de.tectoast.emolga.utils.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.awt.*;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WarnsCommand extends Command {


    public WarnsCommand() {
        super("warns", "`!warns <User>` Zeigt alle Verwarnungen des Users an", CommandCategory.Moderator);
    }

    @Override
    public void process(CommandEvent e) throws Exception {
        Message m = e.getMessage();
        TextChannel tco = e.getChannel();
        String gid = e.getGuild().getId();
        if (m.getMentionedMembers().size() != 1) {
            tco.sendMessage("Du musst einen User taggen!").queue();
            return;
        }
        Member mem = m.getMentionedMembers().get(0);
        ResultSet res = Database.select("select * from warns where userid=" + mem.getId() + " and guildid=" + gid);
        StringBuilder str = new StringBuilder();
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        while(res.next()) {
            str.append("Von: <@").append(res.getLong("modid")).append(">\nGrund: ").append(res.getString("reason")).append("\n").append("Zeitpunkt: ").append(format.format(new Date(res.getTimestamp("timestamp").getTime()))).append(" Uhr\n\n");
        }
        if (str.toString().equals("")) {
            tco.sendMessage("Dieser User hat bisher keine Verwarnungen!").queue();
        } else {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.CYAN);
            builder.setTitle("Verwarnungen von " + mem.getEffectiveName());
            builder.setDescription(str.toString());
            tco.sendMessage(builder.build()).queue();
        }
    }
}
