package de.tectoast.emolga.commands.moderator;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.database.Database;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;

import java.awt.*;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WarnsCommand extends Command {


    public WarnsCommand() {
        super("warns", "Zeigt alle Verwarnungen des Users an", CommandCategory.Moderator);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("user", "User", "User, dessen Verwarnungen gezeigt werden sollen", ArgumentManagerTemplate.DiscordType.USER)
                .setExample("!warns @BöserUser123")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) throws Exception {

        String gid = e.getGuild().getId();

        Member mem = e.getArguments().getMember("user");
        ResultSet res = Database.select("select * from warns where userid=" + mem.getId() + " and guildid=" + gid);
        StringBuilder str = new StringBuilder();
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        while (res.next()) {
            str.append("Von: <@").append(res.getLong("modid")).append(">\nGrund: ").append(res.getString("reason")).append("\n").append("Zeitpunkt: ").append(format.format(new Date(res.getTimestamp("timestamp").getTime()))).append(" Uhr\n\n");
        }
        if (str.toString().equals("")) {
            e.reply(mem.getEffectiveName() + " hat bisher keine Verwarnungen!");
        } else {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.CYAN);
            builder.setTitle("Verwarnungen von " + mem.getEffectiveName());
            builder.setDescription(str.toString());
            e.reply(builder.build());
        }
    }
}
