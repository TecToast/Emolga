package de.tectoast.emolga.domain.league.signup.service.input

import de.tectoast.emolga.domain.league.signup.model.ModalInputOptions
import de.tectoast.emolga.domain.league.signup.model.SignupInput
import de.tectoast.emolga.domain.league.signup.model.SignupValidateResult
import de.tectoast.emolga.utils.json.K18n_SignupInput
import de.tectoast.emolga.utils.k18n
import org.koin.core.annotation.Single


@Single
class OfListHandler : SignupInputHandler<SignupInput.OfList> {
    override val targetClass = SignupInput.OfList::class

    override fun getModalInputOptions(config: SignupInput.OfList): ModalInputOptions {
        return ModalInputOptions(label = config.name.k18n, required = true, list = config.list)
    }

    override suspend fun validate(config: SignupInput.OfList, data: String) = SignupValidateResult.wrapNullable(
        config.list.firstOrNull { it.equals(data, ignoreCase = true) },
        K18n_SignupInput.OF_LIST_Allowed(config.list.joinToString(", ") { opt -> "`$opt`" })
    )

    override fun getDisplayTitle(config: SignupInput.OfList) = config.name.takeIf { config.visibleForAll }?.k18n
}