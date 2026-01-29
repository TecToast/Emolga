package de.tectoast.emolga.features.draft

import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.MessageContextArgs
import de.tectoast.emolga.features.MessageContextFeature
import de.tectoast.emolga.features.MessageContextSpec
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.teamgraphics.GDLStyle
import de.tectoast.emolga.utils.teamgraphics.TeamGraphicGenerator
import de.tectoast.emolga.utils.teamgraphics.toFileUpload
import dev.minn.jda.ktx.messages.editMessage
import dev.minn.jda.ktx.messages.into
import org.litote.kmongo.eq

object UpdateLogoContext : MessageContextFeature(MessageContextSpec("Update Logo (DEV ONLY)")) {
    init {
        restrict(flo)
    }

    private val userRegex = Regex("<@(\\d+)>")

    context(iData: InteractionData)
    override suspend fun exec(e: MessageContextArgs) {
        iData.reply("Updating logo...", ephemeral = true)
        val uid = userRegex.find(e.message.contentRaw)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: return iData.reply(
            "No user found",
            ephemeral = true
        )
        val league = db.league.find(League::tcid eq iData.tc).first() ?: return iData.reply(
            "No league found for this TC",
            ephemeral = true
        )
        val idx = league(uid)
        val style = GDLStyle(league.leaguename.removePrefix("GDLS12"))
        val teamData = TeamGraphicGenerator.TeamData.singleFromLeague(league, idx)
        league.tc.editMessage(
            id = e.message.id,
            attachments = TeamGraphicGenerator.generate(teamData, style).toFileUpload().into()
        ).queue()
    }
}