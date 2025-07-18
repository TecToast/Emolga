package de.tectoast.emolga.features.draft

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.Google
import de.tectoast.emolga.utils.json.LogoChecksum
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get
import dev.minn.jda.ktx.coroutines.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.utils.FileUpload
import org.litote.kmongo.eq
import java.security.MessageDigest

object LogoCommand : CommandFeature<LogoCommand.Args>(
    ::Args, CommandSpec(
        "logo",
        "Reicht dein Logo ein",
        *draftGuilds
    )
) {
    private val logger = KotlinLogging.logger {}
    val allowedFileFormats = setOf("png", "jpg", "jpeg", "webp")

    class Args : Arguments() {
        var logo by attachment("Logo", "Das Logo")
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        insertLogo(e.logo, iData.user)
    }

    context(iData: InteractionData)
    suspend fun insertLogo(logo: Message.Attachment, uid: Long) {
        iData.deferReply(ephemeral = true)
        val lsData = db.signups.get(iData.gid)!!
        if (lsData.logoSettings == null) {
            return iData.reply("In dieser Liga gibt es keine eigenen Logos!", ephemeral = true)
        }
        val signUpData = lsData.getDataByUser(uid) ?: return iData.reply("Du bist nicht angemeldet!", ephemeral = true)
        val logoData = LogoInputData.fromAttachment(logo) ?: return
        lsData.logoSettings.handleLogo(lsData, signUpData, logoData)
        iData.reply("Das Logo wurde erfolgreich hochgeladen!", ephemeral = true)
        val checksum = uploadToCloud(logoData)
        signUpData.logoChecksum = checksum
        lsData.save()
    }

    suspend fun uploadToCloud(
        data: LogoInputData,
        handler: (LogoChecksum) -> Unit = {},
    ) =
        withContext(Dispatchers.IO) {
            val logoData = db.logochecksum.findOne(LogoChecksum::checksum eq data.checksum) ?: run {
                val fileId = Google.uploadFileToDrive(
                    "1-weiL8SEMYPlyXX755qz1TeoaBOSiX0n", data.fileName,
                    "image/${data.fileExtension}", data.bytes
                )
                LogoChecksum(data.checksum, fileId).also { db.logochecksum.insertOne(it) }
            }
            handler(logoData)
            logoData.checksum
        }

    data class LogoInputData(val fileExtension: String, val bytes: ByteArray) {
        val checksum = hashBytes(bytes)
        val fileName = "$checksum.$fileExtension"

        fun toFileUpload() = FileUpload.fromData(bytes, fileName)

        companion object {
            context(iData: InteractionData)
            suspend fun fromAttachment(
                attachment: Message.Attachment,
                ignoreRequirements: Boolean = false
            ): LogoInputData? {
                val fileExtension =
                    attachment.fileExtension?.lowercase()?.takeIf { ignoreRequirements || it in allowedFileFormats }
                        ?: return iData.reply("Das Logo muss eine Bilddatei sein!", ephemeral = true).let { null }
                val bytes = withContext(Dispatchers.IO) {
                    val bytes = try {
                        attachment.proxy.download().await().readAllBytes()
                    } catch (ex: Exception) {
                        logger.error("Couldnt download logo", ex)
                        iData.reply("Das Logo konnte nicht heruntergeladen werden!", ephemeral = true)
                        return@withContext null
                    }
                    if (!ignoreRequirements && bytes.size > 1024 * 1024 * 10) {
                        iData.reply("Das Logo darf nicht größer als 10MB sein!", ephemeral = true)
                        return@withContext null
                    }
                    bytes
                } ?: return null
                return LogoInputData(fileExtension, bytes)

            }
        }
    }


    private fun hashBytes(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).fold("") { str, it ->
        str + "%02x".format(it)
    }.take(15)
}
