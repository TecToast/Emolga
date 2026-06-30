package de.tectoast.emolga.domain.league.signup.service.input

import de.tectoast.emolga.domain.league.signup.model.ModalInputOptions
import de.tectoast.emolga.domain.league.signup.model.SignupInput
import de.tectoast.emolga.domain.league.signup.model.SignupValidateResult
import de.tectoast.emolga.utils.handler.BaseHandler
import de.tectoast.k18n.generated.K18nMessage

interface SignupInputOperations<C : SignupInput> {
    fun getModalInputOptions(config: C): ModalInputOptions
    suspend fun validate(config: C, data: String): SignupValidateResult
    fun getDisplayTitle(config: C): K18nMessage?
    fun mapValueForDisplay(config: C, data: String): String
}

interface SignupInputHandler<C : SignupInput> : BaseHandler<C>, SignupInputOperations<C> {
    override suspend fun validate(config: C, data: String): SignupValidateResult = SignupValidateResult.Success(data)

    override fun getDisplayTitle(config: C): K18nMessage? = null

    override fun mapValueForDisplay(config: C, data: String) = data
}