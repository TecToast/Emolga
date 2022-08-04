package de.tectoast.emolga.commands.moderator

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

class WarnCommand : Command("warn", "Verwarnt den User", CommandCategory.Moderator) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "user",
                "User",
                "Der User, der gekickt werden soll",
                ArgumentManagerTemplate.DiscordType.USER,
                true
            )
            .add("reason", "Grund", "Der Grund des Kicks", ArgumentManagerTemplate.Text.any(), true)
            .setExample("!warn @BöserUser123 Verstoß von Regel XY")
            .build()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val args = e.arguments
        warn(e.textChannel, e.member, args.getMember("user"), args.getOrDefault("reason", "Nicht angegeben"))
    }
}