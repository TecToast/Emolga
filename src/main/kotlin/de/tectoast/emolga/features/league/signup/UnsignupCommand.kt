package de.tectoast.emolga.features.league.signup

import de.tectoast.emolga.domain.league.signup.service.SignupService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.league.K18n_Signup
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.NoArgs
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.msg
import org.koin.core.annotation.Single


@Single(binds = [ListenerProvider::class])
class UnsignupCommand(private val signupService: SignupService) :
    CommandFeature<NoArgs>(NoArgs(), CommandSpec("unsignup", K18n_Signup.UnsignupHelp)) {

    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        iData.reply(
            signupService.removeUser(iData.gid, iData.user).msg(),
            ephemeral = true
        )
    }
}