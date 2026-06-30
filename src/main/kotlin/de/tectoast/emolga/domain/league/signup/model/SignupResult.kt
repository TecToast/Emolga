package de.tectoast.emolga.domain.league.signup.model

import de.tectoast.k18n.generated.K18nMessage

sealed interface SignupResult {
    data class Success(val message: K18nMessage) : SignupResult
    data class ErrorValidation(val errors: List<K18nMessage>) : SignupResult
    data class ErrorSignup(val message: K18nMessage) : SignupResult
    data class SuccessWithLogoError(val message: K18nMessage) : SignupResult
}