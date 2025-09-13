package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.database.exposed.DraftAdminsDB
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.NoArgs
import de.tectoast.emolga.league.League
import de.tectoast.emolga.league.NextPlayerData
import de.tectoast.emolga.league.SkipReason
import net.dv8tion.jda.api.Permission

object SkipPickCommand :
    CommandFeature<NoArgs>(NoArgs(), CommandSpec("skippick", "Skippe eine Person beim Draft")) {

    init {
        restrict {
            val mem = member()
            mem.hasPermission(Permission.ADMINISTRATOR) || DraftAdminsDB.isAdmin(
                mem.guild.idLong,
                mem
            )
        }
    }

    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        League.executeAsNotCurrent(asParticipant = false) {
            afterPickOfficial(NextPlayerData.Moved(SkipReason.SKIP, skippedUser = current, skippedBy = iData.user))
            iData.reply("+1", ephemeral = true)
        }
    }
}
