package de.tectoast.emolga.commands.flo

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.sql.managers.MusicGuildsManager

class EnableMusicCommand : Command("enablemusic", "Enabled Musik auf einem Server", CommandCategory.Flo) {
    override suspend fun process(e: GuildCommandEvent) {
        val id = e.guild.idLong
        MusicGuildsManager.addGuild(id)
        CommandCategory.musicGuilds.add(id)
        e.reply("Alles klar, Meister :)")
    }

    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }
}