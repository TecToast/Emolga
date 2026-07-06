package de.tectoast.emolga.utils

import de.tectoast.emolga.domain.league.signup.model.LogoInputData
import de.tectoast.emolga.domain.league.signup.repository.LogoNameRepository
import de.tectoast.emolga.domain.league.signup.service.logo.LogoCloud
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

/**
 * Utility object for interacting with the StaticCloud API (see https://github.com/TecToast/StaticCloud)
 */
@Single
class StaticCloud(
    private val credentials: BotConfig.StaticCloud,
    private val logoNameRepository: LogoNameRepository,
    val httpClient: HttpClient
) : LogoCloud {
    override val hashLength = credentials.hashLength

    override suspend fun downloadImage(fileName: String): BufferedImage =
        httpClient.get(getImageDownloadLink(fileName)).bodyAsBytes().let {
            ImageIO.read(it.inputStream())
        }


    private fun getImageDownloadLink(fileName: String): String = "${credentials.baseUrl}/i/$fileName"

    /**
     * Uploads the specified file to the specified folder in the StaticCloud
     * @param name The name of the file
     * @param mimeType The MIME type of the file
     * @param data The data of the file
     * @return The ID of the file
     */
    private suspend fun uploadFileToCloud(name: String, mimeType: String, data: ByteArray): String =
        withContext(Dispatchers.IO) {
            httpClient.submitFormWithBinaryData(url = "${credentials.baseUrl}/upload", formData = formData {
                append("file", data, Headers.build {
                    append(HttpHeaders.ContentType, mimeType)
                    append(HttpHeaders.ContentDisposition, "filename=\"$name\"")
                })
                append("path", name)
            }) {
                header("Authorization", "Bearer ${credentials.token}")
            }.bodyAsText().trim('"')
        }

    override suspend fun uploadLogoToCloud(
        data: LogoInputData
    ) = withContext(Dispatchers.IO) {
        val fileName = data.fileName
        if (!logoNameRepository.fileNameExists(data.fileName)) {
            uploadFileToCloud(
                fileName, "image/${data.fileExtension}", data.bytes
            )
        }
        logoNameRepository.insertFileName(fileName, data.teamName)
        fileName
    }
}
