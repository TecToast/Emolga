package de.tectoast.emolga.commands.moderator

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

class MuteCommand : Command("mute", "Mutet den User wegen des angegebenen Grundes", CommandCategory.Moderator) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "user",
                "User",
                "Der User, der gemutet werden soll",
                ArgumentManagerTemplate.DiscordType.USER,
                true
            )
            .add("reason", "Grund", "Der Grund des Mutes", ArgumentManagerTemplate.Text.any(), true)
            .setExample("!mute @BöserUser123 Hat böse Wörter verwendet")
            .build()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val args = e.arguments
        if (!args.has("user")) return
        mute(e.textChannel, e.member, args.getMember("user"), args.getOrDefault("reason", "Nicht angegeben"))
    }
}