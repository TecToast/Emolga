package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.json.emolga.draft.League

object RoundCommand : Command("round", "Zeigt die Runde des derzeitigen Drafts an", CommandCategory.Draft) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override suspend fun process(e: GuildCommandEvent) {
        e.reply(League.onlyChannel(e.textChannel.idLong)?.let { "Der Draft ist in Runde ${it.round}!" }
            ?: "Es läuft zurzeit kein Draft in diesem Channel!")
    }
}
