package de.tectoast.emolga.domain.league.signup.service.input

import de.tectoast.emolga.domain.league.signup.model.SignupInput
import de.tectoast.emolga.domain.league.signup.model.SignupValidateResult
import de.tectoast.emolga.domain.league.signup.model.form.SignupFormField
import de.tectoast.emolga.utils.json.K18n_SignupInput
import de.tectoast.emolga.utils.toShowdownUserId
import org.koin.core.annotation.Single


@Single
class SDNameSignupInputHandler : SignupInputHandler<SignupInput.SDName> {
    override val targetClass = SignupInput.SDName::class

    override fun getFormField(config: SignupInput.SDName, oldData: String?) =
        SignupFormField.TextInputState(
            config.id,
            K18n_SignupInput.SDNAME,
            inputRequired = true,
            requiredLength = 1..18,
            value = oldData
        )

    override suspend fun validate(config: SignupInput.SDName, data: String) = SignupValidateResult.wrapNullable(
        data.takeIf { data.toShowdownUserId().value.length in 1..18 }, K18n_SignupInput.SDNAMEInvalid
    )

    override fun getDisplayTitle(config: SignupInput.SDName) = K18n_SignupInput.SDNAME
}
