package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.database.exposed.DraftAdmins
import de.tectoast.emolga.utils.json.emolga.draft.League
import net.dv8tion.jda.api.Permission

class SkipPickCommand : Command("skippick", "Skippe eine Person beim Draft", CommandCategory.Draft) {
    init {
        setCustomPermissions { mem ->
            mem.hasPermission(Permission.ADMINISTRATOR) || DraftAdmins.isAdmin(mem.guild.idLong, mem)
        }
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        slash(true, *draftGuilds)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val tc = e.textChannel
        val d = League.onlyChannel(tc.idLong) ?: return e.reply(
            "Es läuft zurzeit kein Draft in diesem Channel!",
            ephemeral = true
        )
        d.triggerTimer(League.TimerReason.SKIP, e.member.idLong)
        e.reply("+1", ephemeral = true)
    }
}
