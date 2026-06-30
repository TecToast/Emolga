package de.tectoast.emolga.domain.league.signup.service.logo

import de.tectoast.emolga.domain.league.signup.model.LogoInputData
import de.tectoast.emolga.domain.league.signup.model.form.FileSubmission
import de.tectoast.emolga.utils.CalcResult
import de.tectoast.emolga.utils.json.K18n_SignupInput
import de.tectoast.emolga.utils.teamgraphics.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.koin.core.annotation.Single
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import javax.imageio.ImageIO
import kotlin.math.max

private val logger = KotlinLogging.logger {}
private const val MAX_SIZE = 32

@Single
class LogoService(private val logoCloud: LogoCloud) {

    private val allowedFileFormats = setOf("png", "jpg", "jpeg", "webp")

    suspend fun uploadLogo(data: LogoInputData): String {
        return logoCloud.uploadLogoToCloud(data)
    }

    suspend fun fromAttachment(
        attachment: FileSubmission,
        ignoreRequirements: Boolean = false,
        teamName: String? = null
    ): CalcResult<LogoInputData> = withContext(Dispatchers.IO) {
        attachment.fileExtension.lowercase().takeIf { ignoreRequirements || it in allowedFileFormats }
            ?: return@withContext CalcResult.Error(K18n_SignupInput.LogoMustBeImage)
        val bytes = try {
            attachment.dataProvider()
        } catch (ex: Exception) {
            logger.error("Couldnt download logo", ex)
            return@withContext CalcResult.Error(K18n_SignupInput.LogoDownloadError)
        }
        if (!ignoreRequirements && bytes.size > 1024 * 1024 * MAX_SIZE) {
            return@withContext CalcResult.Error(K18n_SignupInput.LogoTooBig(MAX_SIZE))
        }
        val image = ImageIO.read(bytes.inputStream())
            ?: return@withContext CalcResult.Error(K18n_SignupInput.LogoNoValidImage)
        val imageToUse = if (max(image.width, image.height) > 2000) {
            val scalingFactor = 2000.0 / max(image.width, image.height)
            val newImg = BufferedImage(
                (image.width * scalingFactor).toInt(),
                (image.height * scalingFactor).toInt(),
                BufferedImage.TYPE_INT_ARGB
            )
            val g2d = newImg.createGraphics()
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            g2d.drawImage(image, 0, 0, newImg.width, newImg.height, null)
            g2d.dispose()
            newImg
        } else image
        val croppedImage = ImageUtils.cropToContent(imageToUse)
        val baos = ByteArrayOutputStream()
        ImageIO.write(croppedImage, "png", baos)
        val finalBytes = baos.toByteArray()
        CalcResult.Success(LogoInputData(hashBytes(finalBytes), "png", finalBytes, teamName))
    }

    private fun hashBytes(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).fold("") { str, it ->
        str + "%02x".format(it)
    }.take(logoCloud.hashLength)
}


