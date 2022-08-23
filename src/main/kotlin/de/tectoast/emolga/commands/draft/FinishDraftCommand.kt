package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.saveEmolgaJSON
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.emolga.draft.League

class FinishDraftCommand :
    Command("finishdraft", "Beendet für dich den Draft", CommandCategory.Draft, Constants.G.NDS, Constants.G.ASL) {
    init {
        aliases.add("finish")
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override suspend fun process(e: GuildCommandEvent) {
        e.textChannel
        val memberr = e.member
        memberr.idLong
        val d = League.byChannel(e) ?: return
        if (d.isFinishedForbidden()) {
            e.reply("Dieser Draft unterstützt /finish nicht!")
            return
        }
        val mem = d.current
        d.checkFinishedForbidden(mem)?.let {
            e.reply(it)
            return
        }
        e.reply("Du hast den Draft für dich beendet!")
        d.addFinished(mem)
        d.nextPlayer()
        saveEmolgaJSON()
    }
}