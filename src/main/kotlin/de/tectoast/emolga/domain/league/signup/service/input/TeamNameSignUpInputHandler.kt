package de.tectoast.emolga.domain.league.signup.service.input

import de.tectoast.emolga.domain.league.signup.model.ModalInputOptions
import de.tectoast.emolga.domain.league.signup.model.SignupInput
import de.tectoast.emolga.utils.json.K18n_SignupInput
import org.koin.core.annotation.Single


@Single
class TeamNameSignupInputHandler : SignupInputHandler<SignupInput.TeamName> {
    override val targetClass = SignupInput.TeamName::class

    override fun getModalInputOptions(config: SignupInput.TeamName) =
        ModalInputOptions(label = K18n_SignupInput.TEAMNAME, required = true, requiredLength = 1..100)

    override fun getDisplayTitle(config: SignupInput.TeamName) = K18n_SignupInput.TEAMNAME
}