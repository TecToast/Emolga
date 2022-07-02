package de.tectoast.emolga.commands.moderator

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.sql.managers.WarnsManager
import net.dv8tion.jda.api.EmbedBuilder
import java.awt.Color

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

    override fun process(e: GuildCommandEvent) {
        val gid = e.guild.idLong
        val mem = e.arguments!!.getMember("user")
        val str = WarnsManager.getWarnsFrom(mem.idLong, gid)
        if (str.isEmpty()) {
            e.reply(mem.effectiveName + " hat bisher keine Verwarnungen!")
        } else {
            val builder = EmbedBuilder()
            builder.setColor(Color.CYAN)
            builder.setTitle("Verwarnungen von " + mem.effectiveName)
            builder.setDescription(str)
            e.reply(builder.build())
        }
    }
}