package de.tectoast.emolga.commands.draft.during

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.json.emolga.draft.League

object FinishDraftCommand :
    TestableCommand<NoCommandArgs>("finishdraft", "Beendet für dich den Draft") {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        slash(true, *draftGuilds)
    }

    context (CommandData)
    override suspend fun exec(e: NoCommandArgs) {
        val mem = user
        val d = League.onlyChannel(tc)?.takeIf { mem in it.table } ?: return reply(
            "In diesem Channel läuft kein Draft, an welchem du teilnimmst!",
            ephemeral = true
        )
        if (d.isFinishedForbidden()) return reply("Dieser Draft unterstützt /finishdraft nicht!")
        d.checkFinishedForbidden(mem)?.let {
            return reply(it)
        }
        replyAwait("<@${user}> hat den Draft für sich beendet!")
        d.addFinished(mem)
        if (d.current == mem)
            d.afterPickOfficial()
        d.save()
    }

    override fun fromGuildCommandEvent(e: GuildCommandEvent) = NoCommandArgs
}
