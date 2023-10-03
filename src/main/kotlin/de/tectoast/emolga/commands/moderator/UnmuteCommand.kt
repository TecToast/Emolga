package de.tectoast.emolga.commands.moderator

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

object UnmuteCommand : Command("unmute", "Entmutet den User", CommandCategory.Moderator) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("user", "User", "User, der entmutet werden soll", ArgumentManagerTemplate.DiscordType.USER)
            .setExample("!unmute @BöserUser123")
            .build()
    }

    override suspend fun process(e: GuildCommandEvent) {
        unmute(e.textChannel, e.arguments.getMember("user"))
    }
}
