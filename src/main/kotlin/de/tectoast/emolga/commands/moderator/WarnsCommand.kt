package de.tectoast.emolga.commands.moderator

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.embedColor
import de.tectoast.emolga.utils.sql.managers.WarnsManager
import dev.minn.jda.ktx.messages.Embed

class WarnsCommand : Command("warns", "Zeigt alle Verwarnungen des Users an", CommandCategory.Moderator) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "user",
                "User",
                "User, dessen Verwarnungen gezeigt werden sollen",
                ArgumentManagerTemplate.DiscordType.USER
            )
            .setExample("!warns @BöserUser123")
            .build()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val mem = e.arguments.getMember("user")
        val str = WarnsManager.getWarnsFrom(mem.idLong, e.guild.idLong)
        if (str.isEmpty()) {
            e.reply(mem.effectiveName + " hat bisher keine Verwarnungen!")
        } else {
            e.reply(Embed(title = "Verwarnungen von ${mem.effectiveName}", color = embedColor, description = str))
        }
    }
}