package de.tectoast.emolga.commands.moderator

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import net.dv8tion.jda.api.entities.Invite

class InviteCommand :
    Command("invite", "Erstellt einen einmalig nutzbaren Invite", CommandCategory.Moderator, Constants.ASLID) {
    init {
        addCustomChannel(Constants.ASLID, 773572093697851392L, 736501675447025704L)
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override fun process(e: GuildCommandEvent) {
        e.textChannel.createInvite().setMaxUses(1).flatMap { inv: Invite -> e.textChannel.sendMessage(inv.url) }
            .queue()
    }
}