package de.tectoast.emolga.features.league.signup

import de.tectoast.emolga.domain.league.signup.service.SignupService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.league.K18n_Signup
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.NoArgs
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.isError
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class SignupChangeCommand(private val signupService: SignupService) : CommandFeature<NoArgs>(
    NoArgs(), CommandSpec(
        "signupchange",
        K18n_Signup.SignupChangeHelp,
    )
) {
    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        val result = signupService.trySignupChange(iData.gid, iData.user)
        if (result.isError()) {
            return iData.reply(result.message, ephemeral = true)
        }
        iData.replyModal(result.value.toModal())
    }
}