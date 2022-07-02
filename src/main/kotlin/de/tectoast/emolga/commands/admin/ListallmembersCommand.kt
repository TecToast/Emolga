package de.tectoast.emolga.commands.admin

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import net.dv8tion.jda.api.entities.Member

class ListallmembersCommand : Command("listallmembers", "Zeigt alle Mitglieder des Servers an", CommandCategory.Flo) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        tco.guild.loadMembers().onSuccess { list: List<Member> ->
            val s = StringBuilder()
            for (mem in list) {
                s.append(mem.effectiveName).append("\n")
            }
            tco.sendMessage(s.toString()).queue()
        }
    }
}