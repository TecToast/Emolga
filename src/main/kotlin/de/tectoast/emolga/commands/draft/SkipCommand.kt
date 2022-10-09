package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.json.emolga.draft.League

class SkipCommand : Command(
    "skip",
    "Überspringt deinen Zug",
    CommandCategory.Draft
) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        slash(true, *draftGuilds.toLongArray())
    }

    override suspend fun process(e: GuildCommandEvent) {
        val d = League.byChannel(e) ?: return
        if (!d.isSwitchDraft) {
            e.reply("Dieser Draft ist kein Switch-Draft, daher wird /skip nicht unterstützt!")
            return
        }
        d.replySkip(e)
        d.nextPlayer()
    }
}