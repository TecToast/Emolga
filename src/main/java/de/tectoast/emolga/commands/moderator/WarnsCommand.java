package de.tectoast.emolga.commands.moderator;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.sql.DBManagers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;

import java.awt.*;

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
        long gid = e.getGuild().getIdLong();
        Member mem = e.getArguments().getMember("user");
        String str = DBManagers.WARNS.getWarnsFrom(mem.getIdLong(), gid);
        if (str.isEmpty()) {
            e.reply(mem.getEffectiveName() + " hat bisher keine Verwarnungen!");
        } else {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.CYAN);
            builder.setTitle("Verwarnungen von " + mem.getEffectiveName());
            builder.setDescription(str);
            e.reply(builder.build());
        }
    }
}
