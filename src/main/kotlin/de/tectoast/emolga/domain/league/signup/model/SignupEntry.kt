package de.tectoast.emolga.domain.league.signup.model

data class SignupEntry(
    val users: MutableSet<Long> = mutableSetOf(),
    val data: MutableMap<String, String> = mutableMapOf(),
    var signupMessageId: Long? = null,
    var logoMessageId: Long? = null,
    var logoIdentifier: String? = null,
    var conference: String? = null,
)
