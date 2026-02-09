package de.tectoast.emolga.features.various

import de.tectoast.emolga.database.exposed.GuildManagerDB
import de.tectoast.emolga.features.ButtonFeature
import de.tectoast.emolga.features.ButtonSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.NoArgs
import de.tectoast.generic.K18n_ClickMe
import net.dv8tion.jda.api.components.buttons.ButtonStyle

object GuildAuthorizeButton : ButtonFeature<NoArgs>(NoArgs(), ButtonSpec("guildauthorize")) {

    override val label = K18n_ClickMe
    override val buttonStyle = ButtonStyle.SUCCESS

    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        GuildManagerDB.authorizeUser(iData.gid, iData.user)
        iData.reply(K18n_GuildAuthorizeSuccess, ephemeral = true)
    }
}