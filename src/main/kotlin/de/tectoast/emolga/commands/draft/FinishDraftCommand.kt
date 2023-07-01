package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.json.emolga.draft.League
import dev.minn.jda.ktx.coroutines.await

class FinishDraftCommand :
    Command("finishdraft", "Beendet für dich den Draft", CommandCategory.Draft) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        slash(true, *draftGuilds)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val mem = e.author.idLong
        val d = League.onlyChannel(e.textChannel.idLong)?.takeIf { mem in it.table } ?: return e.reply(
            "In diesem Channel läuft kein Draft, an welchem du teilnimmst!",
            ephemeral = true
        )
        if (d.isFinishedForbidden()) return e.reply("Dieser Draft unterstützt /finishdraft nicht!")
        d.checkFinishedForbidden(mem)?.let {
            return e.reply(it)
        }
        e.slashCommandEvent!!.reply("${e.member.asMention} hat den Draft für sich beendet!").await()
        d.addFinished(mem)
        d.nextPlayer()
        d.save()
    }
}
