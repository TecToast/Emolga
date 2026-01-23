package de.tectoast.emolga.features.various

import de.tectoast.emolga.database.exposed.GuildManagerDB
import de.tectoast.emolga.features.ButtonFeature
import de.tectoast.emolga.features.ButtonSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.NoArgs

object GuildAuthorizeButton : ButtonFeature<NoArgs>(NoArgs(), ButtonSpec("guildauthorize")) {

    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        GuildManagerDB.authorizeUser(iData.gid, iData.user)
        iData.reply("Du wurdest für diesen Server auf der Website autorisiert!", ephemeral = true)
    }
}