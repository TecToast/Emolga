package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.draft.Draft
import net.dv8tion.jda.api.Permission

class SkipPickCommand : Command("skippick", "Skippe eine Person beim Draft", CommandCategory.Draft) {
    init {
        setCustomPermissions { mem ->
            mem.hasPermission(Permission.ADMINISTRATOR) || mem.roles.any { it.id == "702233714360582154" }
        }
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override fun process(e: GuildCommandEvent) {
        val tc = e.textChannel
        val d = Draft.getDraftByChannel(tc)
        d?.timer(Draft.TimerReason.SKIP)
    }
}