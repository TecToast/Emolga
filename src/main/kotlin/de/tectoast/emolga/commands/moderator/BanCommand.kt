package de.tectoast.emolga.commands.moderator

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

class BanCommand : Command("ban", "Bannt den User", CommandCategory.Moderator) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "user",
                "User",
                "Der User, der gebannt werden soll",
                ArgumentManagerTemplate.DiscordType.USER,
                true
            )
            .add("reason", "Grund", "Der Grund des Bans", ArgumentManagerTemplate.Text.any(), true)
            .setExample("!ban @BöserUser123 Hat böse Sachen gemacht")
            .build()
    }

    override fun process(e: GuildCommandEvent) {
        val args = e.arguments!!
        if (!args.has("user")) return
        ban(e.textChannel, e.member, args.getMember("user"), args.getOrDefault("reason", "Nicht angegeben"))
    }
}