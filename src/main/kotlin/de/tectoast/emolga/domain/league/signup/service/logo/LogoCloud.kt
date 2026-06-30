package de.tectoast.emolga.domain.league.signup.service.logo

import de.tectoast.emolga.domain.league.signup.model.LogoInputData
import java.awt.image.BufferedImage

interface LogoCloud {

    val hashLength: Int

    suspend fun downloadImage(fileName: String): BufferedImage

    suspend fun uploadLogoToCloud(
        data: LogoInputData
    ): String
}