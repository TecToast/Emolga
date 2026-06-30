package de.tectoast.emolga.domain.league.signup.model.form

import de.tectoast.k18n.generated.K18nMessage

sealed interface SignupFormField {
    val id: String
    val label: K18nMessage
    val description: K18nMessage?
    val inputRequired: Boolean

    data class TextInputState(
        override val id: String,
        override val label: K18nMessage,
        override val description: K18nMessage? = null,
        override val inputRequired: Boolean,
        val placeholder: K18nMessage?,
        val value: String?
    ) : SignupFormField

    data class SelectInputState(
        override val id: String,
        override val label: K18nMessage,
        override val description: K18nMessage? = null,
        override val inputRequired: Boolean,
        val placeholder: K18nMessage?,
        val list: List<String>,
    ) : SignupFormField

    data class UserSelectState(
        override val id: String,
        override val label: K18nMessage,
        override val description: K18nMessage?,
        override val inputRequired: Boolean = false
    ) : SignupFormField

    data class FileUploadState(
        override val id: String,
        override val label: K18nMessage,
        override val description: K18nMessage?,
        override val inputRequired: Boolean = false
    ) : SignupFormField
}