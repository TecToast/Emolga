package de.tectoast.emolga.commands.draft.during

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.json.emolga.draft.League

object SkipCommand : DraftCommand<NoSpecifiedDraftCommandData>(
    "skip",
    "Überspringt deinen Zug"
) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        slash(true, *draftGuilds)
    }

    override fun fromGuildCommandEvent(e: GuildCommandEvent) = NoSpecifiedDraftCommandData

    context (DraftCommandData)
    override suspend fun exec(e: NoSpecifiedDraftCommandData) {
        val d =
            League.byCommand()?.first ?: return reply(
                "Es läuft zurzeit kein Draft in diesem Channel!",
                ephemeral = true
            )
        if (!d.isSwitchDraft) {
            reply("Dieser Draft ist kein Switch-Draft, daher wird /skip nicht unterstützt!")
            return
        }
        d.replySkip()
        d.afterPickOfficial()
    }
}
