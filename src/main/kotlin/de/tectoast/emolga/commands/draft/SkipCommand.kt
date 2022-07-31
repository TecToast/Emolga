package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.emolga.draft.League

class SkipCommand : Command(
    "skip",
    "Überspringt deinen Zug",
    CommandCategory.Draft,
    Constants.ASLID,
    Constants.FPLID,
    Constants.NDSID
) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override fun process(e: GuildCommandEvent) {
        val ev = DraftEvent(e)
        val d = League.byChannel(e.textChannel, e.member.idLong, ev) ?: return
        if (!d.isSwitchDraft) {
            ev.reply("Dieser Draft ist kein Switch-Draft, daher wird !skip nicht unterstützt!")
            return
        }
        d.nextPlayer()
    }
}