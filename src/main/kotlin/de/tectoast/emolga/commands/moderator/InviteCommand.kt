package de.tectoast.emolga.commands.moderator

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants

class InviteCommand :
    Command("invite", "Erstellt einen einmalig nutzbaren Invite", CommandCategory.Moderator, Constants.ASLID) {
    init {
        addCustomChannel(Constants.ASLID, 773572093697851392L, 736501675447025704L)
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override suspend fun process(e: GuildCommandEvent) {
        e.guild.defaultChannel?.createInvite()?.setMaxUses(1)?.map { e.reply(it.url) }
            ?.queue() ?: e.reply("Kein Default Channel gefunden! Sollte nicht passieren, melde dich bei Flo")
    }
}