package de.tectoast.emolga.commands.draft.during

import de.tectoast.emolga.commands.CommandData
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.NoCommandArgs
import de.tectoast.emolga.commands.TestableCommand
import de.tectoast.emolga.utils.json.emolga.draft.League

object SkipCommand : TestableCommand<NoCommandArgs>(
    "skip",
    "Überspringt deinen Zug"
) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        slash(true, *draftGuilds)
    }

    override fun fromGuildCommandEvent(e: GuildCommandEvent) = NoCommandArgs

    context (CommandData)
    override suspend fun exec(e: NoCommandArgs) {
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