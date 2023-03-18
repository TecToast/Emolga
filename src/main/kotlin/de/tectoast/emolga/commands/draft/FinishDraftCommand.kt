package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.saveEmolgaJSON
import de.tectoast.emolga.utils.json.emolga.draft.League

class FinishDraftCommand :
    Command("finishdraft", "Beendet für dich den Draft", CommandCategory.Draft) {
    init {
        aliases.add("finish")
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        slash(true, *draftGuilds)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val d =
            League.byCommand(e) ?: return e.reply("Es läuft zurzeit kein Draft in diesem Channel!", ephemeral = true)
        if (d.isFinishedForbidden()) {
            e.reply("Dieser Draft unterstützt /finish nicht!")
            return
        }
        val mem = d.current
        d.checkFinishedForbidden(mem)?.let {
            return e.reply(it)
        }
        d.replyFinish(e)
        d.addFinished(mem)
        d.nextPlayer()
        saveEmolgaJSON()
    }
}
