package de.tectoast.emolga.domain.league.signup.service.input

import de.tectoast.emolga.domain.league.signup.model.SignupInput
import de.tectoast.emolga.domain.league.signup.model.SignupValidateResult
import de.tectoast.emolga.domain.league.signup.model.form.SignupFormField
import de.tectoast.emolga.utils.json.K18n_SignupInput
import de.tectoast.emolga.utils.k18n
import org.koin.core.annotation.Single


@Single
class OfListHandler : SignupInputHandler<SignupInput.OfList> {
    override val targetClass = SignupInput.OfList::class

    override fun getFormField(config: SignupInput.OfList, oldData: String?): SignupFormField {
        return SignupFormField.SelectInputState(config.id, config.name.k18n, inputRequired = true, list = config.list)
    }

    override suspend fun validate(config: SignupInput.OfList, data: String) = SignupValidateResult.wrapNullable(
        config.list.firstOrNull { it.equals(data, ignoreCase = true) },
        K18n_SignupInput.OF_LIST_Allowed(config.list.joinToString(", ") { opt -> "`$opt`" })
    )

    override fun getDisplayTitle(config: SignupInput.OfList) = config.name.takeIf { config.visibleForAll }?.k18n
}
