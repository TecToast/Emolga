package de.tectoast.emolga.commands.various

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.embedColor
import de.tectoast.emolga.utils.Constants
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.Embed
import net.dv8tion.jda.api.entities.Member
import java.time.format.DateTimeFormatter

class ServerInfoCommand :
    Command("serverinfo", "Zeigt Infos über diesen Server", CommandCategory.Various, Constants.G.ASL) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val g = e.guild
        val memberList = g.loadMembers().get()
        e.reply(Embed {
            field("Owner", g.retrieveOwner().await().user.effectiveName, true)
            field("Servererstellung", g.timeCreated.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), true)
            field("Anzahl an Rollen", g.roles.size.toString(), true)
            field {
                name = "Member"
                value = "${memberList.size} Member,\n${memberList.count { it.user.isBot }} Bots, ${
                    memberList.count { member: Member -> !member.user.isBot }
                } Menschen"
                inline = true
            }
            field {
                name = "Channel"
                value =
                    "${g.channels.size} insgesamt:\n${g.categories.size} Kategorien\n${g.textChannels.size} Text, ${g.voiceChannels.size} Voice"
                inline = true
            }
            field("Boostlevel", g.boostTier.key.toString(), true)
            field("Anzahl an Boosts", g.boostCount.toString(), true)
            footer("Server Name: " + g.name + " | ServerID: " + g.id)
            color = embedColor
        })
    }
}
