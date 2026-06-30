package de.tectoast.emolga.domain.league.signup.service.input

import de.tectoast.emolga.domain.league.signup.model.ModalInputOptions
import de.tectoast.emolga.domain.league.signup.model.SignupInput
import de.tectoast.emolga.domain.league.signup.model.SignupValidateResult
import de.tectoast.emolga.domain.ytgeneric.service.YouTubeChannelIdService
import de.tectoast.emolga.utils.json.K18n_SignupInput
import de.tectoast.emolga.utils.k18n
import org.koin.core.annotation.Single

@Single
class YTChannelSignupInputHandler(private val ytChannelIdService: YouTubeChannelIdService) :
    SignupInputHandler<SignupInput.YTChannel> {
    override val targetClass = SignupInput.YTChannel::class

    override fun getModalInputOptions(config: SignupInput.YTChannel): ModalInputOptions {
        return ModalInputOptions(
            label = K18n_SignupInput.YTChannelLabel, required = true, placeholder = "https://youtube.com/@tectoast".k18n
        )
    }

    override suspend fun validate(config: SignupInput.YTChannel, data: String): SignupValidateResult {
        val (channelId, handle) = runCatching { ytChannelIdService.mapToChannelId(data) }.getOrNull()
            ?: return SignupValidateResult.Error(K18n_SignupInput.YTChannelInvalid(data))
        return SignupValidateResult.Success(handle?.let { "@$it" } ?: channelId)
    }

    override fun getDisplayTitle(config: SignupInput.YTChannel) = K18n_SignupInput.YTChannelLabel
    override fun mapValueForDisplay(config: SignupInput.YTChannel, data: String) =
        "https://youtube.com/${if (data.startsWith("@")) data else "channel/$data"}"
}