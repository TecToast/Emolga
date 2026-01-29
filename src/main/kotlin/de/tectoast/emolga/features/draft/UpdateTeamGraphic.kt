package de.tectoast.emolga.features.draft

import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.MessageContextArgs
import de.tectoast.emolga.features.MessageContextFeature
import de.tectoast.emolga.features.MessageContextSpec
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.teamgraphics.TeamGraphicGenerator
import org.litote.kmongo.eq

object UpdateTeamGraphic : MessageContextFeature(MessageContextSpec("Update Teamgraphic (DEV ONLY)")) {
    init {
        restrict(flo)
    }

    private val userRegex = Regex("<@(\\d+)>")

    context(iData: InteractionData)
    override suspend fun exec(e: MessageContextArgs) {
        iData.reply("Updating teamgraphic...", ephemeral = true)
        val uid = userRegex.find(e.message.contentRaw)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: return iData.reply(
            "No user found",
            ephemeral = true
        )
        val league = db.league.find(League::tcid eq iData.tc).first() ?: return iData.reply(
            "No league found for this TC",
            ephemeral = true
        )
        val idx = league(uid)
        TeamGraphicGenerator.editTeamGraphicForLeague(league, idx)
    }
}