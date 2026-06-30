package de.tectoast.emolga.domain.league.signup.model.form

import de.tectoast.k18n.generated.K18nMessage

data class SignupFormState(
    val customId: String, val title: K18nMessage, val fields: List<SignupFormField>
)
