package de.tectoast.emolga.commands.various;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ServerInfoCommand extends Command {
    public ServerInfoCommand() {
        super("serverinfo", "Zeigt Infos über diesen Server", CommandCategory.Various, Constants.ASLID);
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        EmbedBuilder builder = new EmbedBuilder();
        Guild g = e.getGuild();
        List<Member> memberList = g.loadMembers().get();
        builder.addField("Owner", g.retrieveOwner().complete().getUser().getAsTag(), true);
        builder.addField("Servererstellung", g.getTimeCreated().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), true);
        builder.addField("Anzahl an Rollen", String.valueOf(g.getRoles().size()), true);
        builder.addField("Member", memberList.size() + " Member,\n" + memberList.stream().filter(member -> member.getOnlineStatus() != OnlineStatus.OFFLINE).count() + " online\n"
                + memberList.stream().filter(member -> member.getUser().isBot()).count() + " Bots, " + memberList.stream().filter(member -> !member.getUser().isBot()).count() + " Menschen", true);
        builder.addField("Channel", g.getChannels().size() + " insgesamt:\n" + g.getCategories().size() + " Kategorien\n" + g.getTextChannels().size() + " Text, " + g.getVoiceChannels().size() + " Voice", true);
        builder.addField("Boostlevel", String.valueOf(g.getBoostTier().getKey()), true);
        builder.addField("Anzahl an Boosts", String.valueOf(g.getBoostCount()), true);
        builder.setFooter("Server Name: " + g.getName() + " | ServerID: " + g.getId());
        builder.setColor(Color.CYAN);
        e.getChannel().sendMessageEmbeds(builder.build()).queue();
    }
}
