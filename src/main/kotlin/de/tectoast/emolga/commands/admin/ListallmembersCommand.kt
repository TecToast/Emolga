package de.tectoast.emolga.commands.admin

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

class ListallmembersCommand : Command("listallmembers", "Zeigt alle Mitglieder des Servers an", CommandCategory.Flo) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        tco.guild.loadMembers().onSuccess { list ->
            e.reply(list.joinToString("\n") { it.effectiveName })
        }
    }
}