package de.tectoast.emolga.domain.league.signup.service.input

import de.tectoast.emolga.domain.league.signup.model.SignupInput
import de.tectoast.emolga.domain.league.signup.model.form.SignupFormField
import de.tectoast.emolga.utils.json.K18n_SignupInput
import org.koin.core.annotation.Single


@Single
class UserSignupInputHandler : SignupInputHandler<SignupInput.User> {
    override val targetClass = SignupInput.User::class

    override fun getFormField(config: SignupInput.User, oldData: String?): SignupFormField {
        return SignupFormField.UserSelectState(
            config.id,
            getDisplayTitle(config),
            description = null,
            inputRequired = true,
            range = 1..1
        )
    }

    override fun getDisplayTitle(config: SignupInput.User) = K18n_SignupInput.User(config.num)

    override fun mapValueForDisplay(
        config: SignupInput.User,
        data: String
    ) = "<@$data>"
}
