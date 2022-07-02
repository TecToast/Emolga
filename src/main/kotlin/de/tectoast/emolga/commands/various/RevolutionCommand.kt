package de.tectoast.emolga.commands.various

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.jsolf.JSONObject
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Member
import java.util.*

class RevolutionCommand : Command("revolution", "muhahahahaha", CommandCategory.Various) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("name", "Name", "Name der Revolution", ArgumentManagerTemplate.Text.any())
            .setExample("!revolution Emolga")
            .build()
        setCustomPermissions(PermissionPreset.CULT)
    }

    override fun process(e: GuildCommandEvent) {
        if (e.argsLength == 0) {
            e.reply("Du musst einen Revolution-Namen angeben!")
            return
        }
        val name = e.getArg(0)
        val g = e.guild
        val arr = emolgaJSON.getJSONArray("activerevolutions")
        val isRevo = arr.toList().contains(g.idLong)
        val o = JSONObject()
        e.textChannel.sendMessage("Möge die **$name-Revolution** beginnen! :D").queue()
        g.loadMembers().onSuccess { list: List<Member> ->
            for (member in list) {
                if (member.isOwner) continue
                if (member.id == e.jda.selfUser.id) member.modifyNickname(name + "leader").queue()
                if (!g.selfMember.canInteract(member)) continue
                if (!isRevo) o.put(member.id, member.effectiveName)
                member.modifyNickname(name).queue()
            }
            for (gc in g.channels) {
                gc.manager.setName((if (gc.type == ChannelType.TEXT) name.lowercase(Locale.getDefault()) else name) + "-" + gc.name)
                    .queue()
            }
            if (!isRevo) {
                emolgaJSON.getJSONObject("revolutionreset").put(g.id, o)
                arr.put(g.idLong)
            }
            saveEmolgaJSON()
        }
    }
}