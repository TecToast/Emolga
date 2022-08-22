package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.saveEmolgaJSON
import de.tectoast.emolga.utils.json.Emolga
import kotlinx.coroutines.cancel

class StopdraftCommand : Command("stopdraft", "Beendet den Draft", CommandCategory.Flo) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("draftchannel", "Draftchannel", "Der Channel des Drafts", ArgumentManagerTemplate.DiscordType.CHANNEL)
            .setExample("!stopdraft #stufe1-draft")
            .build()
    }

    override suspend fun process(e: GuildCommandEvent) =
        e.reply(if (Emolga.get.drafts.values.any { l ->
                (l.tc.idLong == e.arguments.getChannel("draftchannel").idLong).also {
                    if (it) {
                        l.cooldownJob!!.cancel("Draft stopped by command")
                        l.isRunning = false
                        saveEmolgaJSON()
                    }
                }
            }) "Der Draft wurde beendet!" else "In diesem Channel läuft derzeit kein Draft!")
}