package de.tectoast.emolga.commands.showdown

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.database.exposed.SpoilerTagsDB

class SpoilerTagsCommand : Command(
    "spoilertags",
    "Aktiviert oder deaktiviert den Spoilerschutz bei Showdown-Ergebnissen. (Gilt serverweit)",
    CommandCategory.Showdown
) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        everywhere = true
        slash(false, -1)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val gid = e.guild.idLong
        if (SpoilerTagsDB.delete(gid)) {
            e.reply("Auf diesem Server sind Spoiler-Tags bei Showdown-Ergebnissen nun **deaktiviert**!")
            spoilerTags.remove(gid)
            return
        }
        SpoilerTagsDB.insert(gid)
        spoilerTags.add(gid)
        e.reply("Auf diesem Server sind Spoiler-Tags bei Showdown-Ergebnissen nun **aktiviert**!")
    }
}
