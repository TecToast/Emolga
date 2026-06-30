package de.tectoast.emolga.domain.league.signup.model.form

import de.tectoast.emolga.domain.league.signup.model.SignupResult

sealed interface SignupButtonResult {
    data class Form(val formState: SignupFormState) : SignupButtonResult
    data class DirectSignup(val signupResult: SignupResult) : SignupButtonResult
}