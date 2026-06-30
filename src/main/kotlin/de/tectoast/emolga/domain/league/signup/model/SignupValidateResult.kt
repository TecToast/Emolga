package de.tectoast.emolga.domain.league.signup.model

import de.tectoast.k18n.generated.K18nMessage

sealed interface SignupValidateResult {
    data class Success(val data: String) : SignupValidateResult
    data class Error(val message: K18nMessage) : SignupValidateResult

    companion object {
        fun wrapNullable(msg: String?, errorMsg: K18nMessage) = msg?.let { Success(it) } ?: Error(errorMsg)
    }
}
