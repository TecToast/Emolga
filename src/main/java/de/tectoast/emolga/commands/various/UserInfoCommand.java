package de.tectoast.emolga.commands.various;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.database.Database;
import de.tectoast.emolga.utils.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class UserInfoCommand extends Command {
    public UserInfoCommand() {
        super("userinfo", "Zeigt die Userinfo von dir bzw. dem gepingten Spieler an", CommandCategory.Various);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("user", "User", "Der User, dessen Info du haben möchtest, oder gar nichts, wenn du deine eigene Info sehen möchtest", ArgumentManagerTemplate.DiscordType.USER, true)
                .setExample("!userinfo @Flo")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) throws SQLException {
        ArgumentManager args = e.getArguments();
        Member member = args.has("user") ? args.getMember("user") : e.getMember();
        User u = member.getUser();
        ResultSet set = Database.select("select count(*) as warncount from warns where guildid = " + Constants.ASLID + " and userid = " + u.getId());
        set.next();
        SimpleDateFormat format = new SimpleDateFormat();
        e.getChannel().sendMessageEmbeds(
                new EmbedBuilder()
                        .addField("Userinfo Command für " + u.getAsTag(), "UserID | " + u.getId(), true)
                        .addField("User Status", CommandCategory.Admin.allowsMember(member) ? "Admin" : CommandCategory.Moderator.allowsMember(member) ? "Moderator" : "User", true)
                        .addField("Server Warns", String.valueOf(set.getInt("warncount")), true)
                        .addField("Serverbeitritt", e.getGuild().retrieveMember(u).complete().getTimeJoined().format(DateTimeFormatter.ofPattern("dd.MM.yyyy hh:mm")) + " Uhr", true)
                        .addField("Discordbeitritt", u.getTimeCreated().format(DateTimeFormatter.ofPattern("dd.MM.yyyy hh:mm")) + " Uhr", true)
                        .addField("Boostet seit", member.getTimeBoosted() == null ? "Boostet nicht" : format.format(new Date(member.getTimeBoosted().toEpochSecond())), true)
                        .setFooter("Aufgerufen von " + e.getMember().getUser().getAsTag() + " | " + e.getMember().getId())
                        .setColor(Color.CYAN)
                        .setThumbnail(u.getEffectiveAvatarUrl()).build()).queue();

    }
}
