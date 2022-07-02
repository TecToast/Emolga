package de.tectoast.emolga.commands.flo

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.jsolf.JSONObject
import net.dv8tion.jda.api.entities.Member

class EmolgaDiktaturCommand : Command("emolgadiktatur", "EMOLGADIKTATUR", CommandCategory.Flo) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override fun process(e: GuildCommandEvent) {
        val g = e.guild
        val members = JSONObject()
        e.textChannel.sendMessage("**Möge die Emolga-Diktatur beginnen!**").queue()
        g.loadMembers().onSuccess { list: List<Member> ->
            for (member in list) {
                if (member.isOwner) continue
                if (member.id == e.jda.selfUser.id) member.modifyNickname("Diktator").queue()
                if (!g.selfMember.canInteract(member)) continue
                members.put(member.id, member.effectiveName)
                member.modifyNickname("Emolga-Anhänger").queue()
            }
            for (gc in g.channels) {
                gc.manager.setName("Emolga-" + gc.name).queue()
            }
            emolgaJSON.getJSONObject("emolgareset").put(g.id, members)
            saveEmolgaJSON()
        }
    }
}