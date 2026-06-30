package de.tectoast.emolga.features.various

import de.tectoast.emolga.domain.league.admin.repository.GuildManagerRepository
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.ButtonSpec
import de.tectoast.emolga.features.system.NoArgs
import de.tectoast.emolga.features.system.types.ButtonFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.generic.K18n_ClickMe
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class GuildAuthorizeButton(private val guildManagerRepo: GuildManagerRepository) :
    ButtonFeature<NoArgs>(NoArgs(), ButtonSpec("guildauthorize")) {

    override val label = K18n_ClickMe
    override val buttonStyle = ButtonStyle.SUCCESS

    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        guildManagerRepo.authorizeUser(iData.gid, iData.user)
        iData.reply(K18n_GuildAuthorizeSuccess, ephemeral = true)
    }
}
