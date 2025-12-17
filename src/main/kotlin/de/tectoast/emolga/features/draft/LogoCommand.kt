package de.tectoast.emolga.features.draft

import de.tectoast.emolga.database.exposed.LogoChecksumDB
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.Google
import de.tectoast.emolga.utils.json.LogoChecksum
import de.tectoast.emolga.utils.json.LogoInputData
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.Message

object LogoCommand : CommandFeature<LogoCommand.Args>(
    ::Args, CommandSpec(
        "logo",
        "Reicht dein Logo ein",
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
        lsData.insertLogo(uid, logo)
        iData.reply("Dein Logo wurde erfolgreich hochgeladen!")
    }

    suspend fun uploadToCloud(
        data: LogoInputData,
        handler: (LogoChecksum) -> Unit = {},
    ) =
        withContext(Dispatchers.IO) {
            val logoData = LogoChecksumDB.findByChecksum(data.checksum) ?: run {
                val fileId = Google.uploadFileToDrive(
                    "1-weiL8SEMYPlyXX755qz1TeoaBOSiX0n", data.fileName,
                    "image/${data.fileExtension}", data.bytes
                )
                LogoChecksum(data.checksum, fileId).also { LogoChecksumDB.insertData(it) }
            }
            handler(logoData)
            logoData.checksum
        }


}
