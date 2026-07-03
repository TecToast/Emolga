package de.tectoast.emolga.features.league.signup

import de.tectoast.emolga.domain.league.signup.service.SignupService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.ButtonSpec
import de.tectoast.emolga.features.system.NoArgs
import de.tectoast.emolga.features.system.types.ButtonFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.msg
import de.tectoast.generic.K18n_SignoutVerb
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class SignoutButton(private val service: SignupService) : ButtonFeature<NoArgs>(NoArgs(), ButtonSpec("signout")) {

    override val label = K18n_SignoutVerb
    override val buttonStyle = ButtonStyle.DANGER

    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        iData.reply(service.removeUser(iData.gid, iData.user).msg(), ephemeral = true)
    }
}