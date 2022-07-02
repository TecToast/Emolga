package de.tectoast.emolga.commands.showdown

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.sql.managers.SpoilerTagsManager

class SpoilerTagsCommand : Command(
    "spoilertags",
    "Aktiviert oder deaktiviert den Spoilerschutz bei Showdown-Ergebnissen. (Gilt serverweit)",
    CommandCategory.Showdown
) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        everywhere = true
    }

    override fun process(e: GuildCommandEvent) {
        val gid = e.guild.idLong
        if (SpoilerTagsManager.delete(gid)) {
            e.textChannel.sendMessage("Auf diesem Server sind Spoiler-Tags bei Showdown-Ergebnissen nun **deaktiviert**!")
                .queue()
            spoilerTags.remove(gid)
            return
        }
        SpoilerTagsManager.insert(gid)
        spoilerTags.add(gid)
        e.textChannel.sendMessage("Auf diesem Server sind Spoiler-Tags bei Showdown-Ergebnissen nun **aktiviert**!")
            .queue()
    }
}