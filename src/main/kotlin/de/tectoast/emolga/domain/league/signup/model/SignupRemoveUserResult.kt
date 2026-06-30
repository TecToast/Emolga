package de.tectoast.emolga.domain.league.signup.model

sealed interface SignupRemoveUserResult {
    data object NotFound : SignupRemoveUserResult
    data class Removed(val signupId: Int, val entry: SignupEntry, val deletedEntry: Boolean) : SignupRemoveUserResult
}