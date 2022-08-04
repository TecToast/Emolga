package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.json.emolga.draft.League

class StopdraftCommand : Command("stopdraft", "Beendet den Draft", CommandCategory.Flo) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("draftchannel", "Draftchannel", "Der Channel des Drafts", ArgumentManagerTemplate.DiscordType.CHANNEL)
            .setExample("!stopdraft #stufe1-draft")
            .build()
    }

    override suspend fun process(e: GuildCommandEvent) =
        e.reply(if (League.drafts.removeIf { l ->
                (l.tc.idLong == e.arguments.getChannel("draftchannel").idLong).also {
                    if (it) l.cooldownFuture!!.cancel(
                        false
                    )
                }
            }) "Der Draft wurde beendet!" else "In diesem Channel läuft derzeit kein Draft!")
}