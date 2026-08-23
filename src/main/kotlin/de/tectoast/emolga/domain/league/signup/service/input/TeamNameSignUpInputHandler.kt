package de.tectoast.emolga.domain.league.signup.service.input

import de.tectoast.emolga.domain.league.signup.model.SignupInput
import de.tectoast.emolga.domain.league.signup.model.form.SignupFormField
import de.tectoast.emolga.utils.json.K18n_SignupInput
import org.koin.core.annotation.Single


@Single
class TeamNameSignupInputHandler : SignupInputHandler<SignupInput.TeamName> {
    override val targetClass = SignupInput.TeamName::class

    override fun getFormField(config: SignupInput.TeamName, oldData: String?): SignupFormField {
        return SignupFormField.TextInputState(
            config.id,
            K18n_SignupInput.TEAMNAME,
            inputRequired = true,
            requiredLength = 1..100,
            value = oldData
        )
    }

    override fun getDisplayTitle(config: SignupInput.TeamName) = K18n_SignupInput.TEAMNAME
}
