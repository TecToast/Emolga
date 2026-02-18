package de.tectoast.emolga.features.draft

import de.tectoast.emolga.database.exposed.TeamGraphicMessageDB
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.MessageContextArgs
import de.tectoast.emolga.features.MessageContextFeature
import de.tectoast.emolga.features.MessageContextSpec
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.teamgraphics.TeamGraphicGenerator

object UpdateTeamGraphic : MessageContextFeature(MessageContextSpec("Update Teamgraphic (DEV ONLY)")) {
    init {
        restrict(flo)
    }

    private val userRegex = Regex("<@(\\d+)>")

    context(iData: InteractionData)
    override suspend fun exec(e: MessageContextArgs) {
        val result = TeamGraphicMessageDB.getByMessageId(e.message.idLong)
            ?: return iData.reply("No teamgraphic found for this message.", ephemeral = true)
        val (leagueName, idx) = result
        iData.reply("Updating teamgraphic...", ephemeral = true)
        TeamGraphicGenerator.editTeamGraphicForLeague(db.league(leagueName), idx)
    }
}