package de.tectoast.emolga.commands.various

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Member
import java.awt.Color
import java.time.format.DateTimeFormatter

class ServerInfoCommand :
    Command("serverinfo", "Zeigt Infos über diesen Server", CommandCategory.Various, Constants.ASLID) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override fun process(e: GuildCommandEvent) {
        val builder = EmbedBuilder()
        val g = e.guild
        val memberList = g.loadMembers().get()
        builder.addField("Owner", g.retrieveOwner().complete().user.asTag, true)
        builder.addField("Servererstellung", g.timeCreated.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), true)
        builder.addField("Anzahl an Rollen", g.roles.size.toString(), true)
        builder.addField(
            "Member", """${memberList.size} Member,
${memberList.stream().filter { member: Member -> member.onlineStatus != OnlineStatus.OFFLINE }.count()} online
${memberList.stream().filter { member: Member -> member.user.isBot }.count()} Bots, ${
                memberList.stream().filter { member: Member -> !member.user.isBot }.count()
            } Menschen""", true
        )
        builder.addField(
            "Channel", """${g.channels.size} insgesamt:
${g.categories.size} Kategorien
${g.textChannels.size} Text, ${g.voiceChannels.size} Voice""", true
        )
        builder.addField("Boostlevel", g.boostTier.key.toString(), true)
        builder.addField("Anzahl an Boosts", g.boostCount.toString(), true)
        builder.setFooter("Server Name: " + g.name + " | ServerID: " + g.id)
        builder.setColor(Color.CYAN)
        e.textChannel.sendMessageEmbeds(builder.build()).queue()
    }
}