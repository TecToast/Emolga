package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.League
import kotlinx.coroutines.cancel
import org.litote.kmongo.eq

class StopdraftCommand : Command("stopdraft", "Beendet den Draft", CommandCategory.Flo) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("draftchannel", "Draftchannel", "Der Channel des Drafts", ArgumentManagerTemplate.DiscordType.CHANNEL)
            .setExample("!stopdraft #stufe1-draft")
            .build()
    }

    override suspend fun process(e: GuildCommandEvent) {
        e.reply(
            db.drafts.find(League::tcid eq e.arguments.getChannel("draftchannel").idLong).first()?.run {
                cooldownJob!!.cancel("Draft stopped by command")
                isRunning = false
                save()
                "Der Draft wurde beendet!"
            } ?: "In diesem Channel läuft derzeit kein Draft!"
        )
    }
}
