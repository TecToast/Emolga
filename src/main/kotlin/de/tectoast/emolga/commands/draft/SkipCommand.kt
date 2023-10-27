package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.json.emolga.draft.League

object SkipCommand : Command(
    "skip",
    "Überspringt deinen Zug",
    CommandCategory.Draft
) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        slash(true, *draftGuilds)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val d =
            League.byCommand(e) ?: return e.reply("Es läuft zurzeit kein Draft in diesem Channel!", ephemeral = true)
        if (!d.isSwitchDraft) {
            e.reply("Dieser Draft ist kein Switch-Draft, daher wird /skip nicht unterstützt!")
            return
        }
        d.replySkip(e)
        d.afterPickOfficial()
    }
}
