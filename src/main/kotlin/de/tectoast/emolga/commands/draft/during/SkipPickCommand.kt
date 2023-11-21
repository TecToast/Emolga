package de.tectoast.emolga.commands.draft.during

import de.tectoast.emolga.commands.CommandData
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.NoCommandArgs
import de.tectoast.emolga.commands.TestableCommand
import de.tectoast.emolga.database.exposed.DraftAdminsDB
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.NextPlayerData
import de.tectoast.emolga.utils.json.emolga.draft.SkipReason
import net.dv8tion.jda.api.Permission

object SkipPickCommand : TestableCommand<NoCommandArgs>("skippick", "Skippe eine Person beim Draft") {
    init {
        setCustomPermissions { mem ->
            mem.hasPermission(Permission.ADMINISTRATOR) || DraftAdminsDB.isAdmin(mem.guild.idLong, mem)
        }
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        slash(true, *draftGuilds)
    }

    override fun fromGuildCommandEvent(e: GuildCommandEvent) = NoCommandArgs
    context (CommandData)
    override suspend fun exec(e: NoCommandArgs) {
        val d = League.onlyChannel(tc) ?: return reply(
            "Es läuft zurzeit kein Draft in diesem Channel!",
            ephemeral = true
        )
        d.afterPickOfficial(NextPlayerData.Moved(SkipReason.SKIP, skippedBy = user))
        reply("+1", ephemeral = true)
    }
}
