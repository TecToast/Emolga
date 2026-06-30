package de.tectoast.emolga.domain.league.signup.model

import net.dv8tion.jda.api.utils.FileUpload

class LogoInputData(hash: String, val fileExtension: String, val bytes: ByteArray, val teamName: String? = null) {
    val fileName = "$hash.$fileExtension"

    fun toFileUpload() = FileUpload.fromData(bytes, fileName)
}