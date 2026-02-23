package de.tectoast.emolga.utils

import de.tectoast.emolga.database.exposed.LogoNameDB
import de.tectoast.emolga.utils.json.LogoInputData
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

/**
 * Utility object for interacting with the StaticCloud API (see https://github.com/TecToast/StaticCloud)
 */
object StaticCloud {
    private lateinit var token: String
    private lateinit var baseUrl: String
    var hashLength: Int = 24
        private set

    fun init(token: String, baseUrl: String, hashLength: Int) {
        this.token = token
        this.baseUrl = baseUrl
        this.hashLength = hashLength
    }

    suspend fun downloadImage(fileName: String): BufferedImage =
        httpClient.get(getImageDownloadLink(fileName)).bodyAsBytes().let {
            ImageIO.read(it.inputStream())
        }


    fun getImageDownloadLink(fileName: String): String = "$baseUrl/i/$fileName"

    /**
     * Uploads the specified file to the specified folder in the StaticCloud
     * @param name The name of the file
     * @param mimeType The MIME type of the file
     * @param data The data of the file
     * @return The ID of the file
     */
    suspend fun uploadFileToCloud(name: String, mimeType: String, data: ByteArray): String =
        withContext(Dispatchers.IO) {
            httpClient.submitFormWithBinaryData(url = "$baseUrl/upload", formData = formData {
                append("file", data, Headers.build {
                    append(HttpHeaders.ContentType, mimeType)
                    append(HttpHeaders.ContentDisposition, "filename=\"$name\"")
                })
                append("hash_length", hashLength)
            }) {
                header("Authorization", "Bearer $token")
            }.bodyAsText().trim('"')
        }

    suspend fun uploadLogoToCloud(
        data: LogoInputData
    ) = withContext(Dispatchers.IO) {
        val fileName = data.fileName
        if (!LogoNameDB.fileNameExists(data.fileName)) {
            uploadFileToCloud(
                data.fileName, "image/${data.fileExtension}", data.bytes
            )
        }
        LogoNameDB.insertFileName(fileName, data.teamName)
        fileName
    }
}