package de.tectoast.emolga.features.league.signup

import de.tectoast.emolga.domain.league.signup.model.form.SignupButtonResult
import de.tectoast.emolga.domain.league.signup.service.SignupService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.ButtonSpec
import de.tectoast.emolga.features.system.types.ButtonFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.isError
import de.tectoast.generic.K18n_SignupVerb
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.emoji.Emoji
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class SignupButton(private val signupService: SignupService) : ButtonFeature<SignupButton.Args>(
    ::Args,
    ButtonSpec("signup")
) {
    override val buttonStyle = ButtonStyle.PRIMARY
    override val label = K18n_SignupVerb
    override val emoji = Emoji.fromUnicode("✅")

    class Args : Arguments() {
        var identifier by string().compIdOnly().nullable()
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        val buttonResult = signupService.handleSignupClick(iData.gid, e.identifier.orEmpty(), iData.user)
        if (buttonResult.isError()) {
            return iData.reply(buttonResult.message, ephemeral = true)
        }
        when (val result = buttonResult.value) {
            is SignupButtonResult.Form -> {
                iData.replyModal(result.formState.toModal())
            }

            is SignupButtonResult.DirectSignup -> {
                iData.reply(result.signupResult.toMessageContent(), ephemeral = true)
            }
        }
    }
}