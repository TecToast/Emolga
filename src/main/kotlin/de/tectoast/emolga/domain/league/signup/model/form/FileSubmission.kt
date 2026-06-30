package de.tectoast.emolga.domain.league.signup.model.form

data class FileSubmission(
    val fileExtension: String, val dataProvider: suspend () -> ByteArray
)