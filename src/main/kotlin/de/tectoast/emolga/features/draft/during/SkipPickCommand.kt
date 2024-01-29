package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.commands.InteractionData
import de.tectoast.emolga.database.exposed.DraftAdminsDB
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.NoArgs
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.NextPlayerData
import de.tectoast.emolga.utils.json.emolga.draft.SkipReason
import net.dv8tion.jda.api.Permission

object SkipPickCommand :
    CommandFeature<NoArgs>(NoArgs(), CommandSpec("skippick", "Skippe eine Person beim Draft", *draftGuilds)) {

    init {
        restrict {
            val mem = member()
            mem.hasPermission(Permission.ADMINISTRATOR) || DraftAdminsDB.isAdmin(mem.guild.idLong, mem)
        }
    }

    context(InteractionData)
    override suspend fun exec(e: NoArgs) {
        val d = League.onlyChannel(tc) ?: return reply(
            "Es läuft zurzeit kein Draft in diesem Channel!",
            ephemeral = true
        )
        d.afterPickOfficial(NextPlayerData.Moved(SkipReason.SKIP, skippedBy = user))
        reply("+1", ephemeral = true)
    }
}
