package de.tectoast.emolga.features.draft

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.commands.InteractionData
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.Google
import de.tectoast.emolga.utils.json.*
import dev.minn.jda.ktx.coroutines.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.utils.FileUpload
import org.litote.kmongo.*
import java.security.MessageDigest

object LogoCommand : CommandFeature<LogoCommand.Args>(
    ::Args, CommandSpec(
        "logo",
        "Reicht dein Logo ein",
        Constants.G.FLP,
        Constants.G.ASL,
        665600405136211989,
        Constants.G.WFS,
        Constants.G.ADK,
        Constants.G.NDS
    )
) {
    val allowedFileFormats = setOf("png", "jpg", "jpeg")

    class Args : Arguments() {
        var logo by attachment("Logo", "Das Logo")
    }

    context(InteractionData)
    override suspend fun exec(e: Args) {
        insertLogo(e.logo, user)
    }

    context(InteractionData)
    suspend fun insertLogo(logo: Message.Attachment, uid: Long) {
        deferReply(ephemeral = true)
        val ligaStartData = db.signups.get(gid)!!
        if (uid !in ligaStartData.users) {
            return reply("Du bist nicht angemeldet!", ephemeral = true)
        }
        if (ligaStartData.noTeam) {
            return reply("In dieser Liga gibt es keine eigenen Teams!", ephemeral = true)
        }
        val fileExtension = logo.fileExtension?.lowercase()?.takeIf { it in allowedFileFormats }
            ?: return reply("Das Logo muss eine Bilddatei sein!", ephemeral = true)
        val (bytes, checksum) = withContext(Dispatchers.IO) {
            val bytes = try {
                logo.proxy.download().await().readAllBytes()
            } catch (ex: Exception) {
                ex.printStackTrace()
                reply("Das Logo konnte nicht heruntergeladen werden!", ephemeral = true)
                return@withContext null
            }
            if (bytes.size > 1024 * 1024 * 3) {
                reply("Das Logo darf nicht größer als 3MB sein!", ephemeral = true)
                return@withContext null
            }
            val checksum = hashBytes(bytes)
            bytes to checksum
        } ?: return
        reply("Das Logo wurde erfolgreich hochgeladen!", ephemeral = true)
        val fileName = "$checksum.$fileExtension"
        val signUpData = ligaStartData.users[uid]!!
        val tc = jda.getTextChannelById(ligaStartData.logoChannel)!!
        signUpData.logomid?.let {
            tc.deleteMessageById(it).queue(null) {}
        }
        val logoMid = tc.sendMessage(
            "**Logo von ${
                listOf(
                    uid,
                    *signUpData.teammates.toTypedArray()
                ).joinToString(" & ") { "<@$it>" }
            } (${signUpData.teamname}):**"
        )
            .addFiles(FileUpload.fromData(bytes, fileName)).await().idLong
        uploadToCloud(
            bytes,
            fileExtension,
            gid,
            uid,
            logoMid,
            checksum,
            fileName
        )
    }

    suspend fun uploadToCloud(
        bytes: ByteArray,
        fileExtension: String,
        gid: Long,
        uid: Long,
        logoMid: Long,
        checksum: String = hashBytes(bytes),
        fileName: String = "$checksum.$fileExtension",
    ) =
        withContext(Dispatchers.IO) {
            val logoData = db.logochecksum.findOne(LogoChecksum::checksum eq checksum) ?: run {
                val fileId = Google.uploadFileToDrive(
                    "1-weiL8SEMYPlyXX755qz1TeoaBOSiX0n", fileName,
                    "image/${fileExtension}", bytes
                )
                LogoChecksum(checksum, fileId).also { db.logochecksum.insertOne(it) }
            }
            val url = "https://drive.google.com/uc?export=download&id=${logoData.fileId}"
            jda.getTextChannelById(1180631129603190824)!!.sendMessage("`$url`\n$url").queue()
            db.signups.updateOne(
                LigaStartData::guild eq gid,
                set(
                    LigaStartData::users.keyProjection(uid) / SignUpData::logoUrl setTo checksum,
                    LigaStartData::users.keyProjection(uid) / SignUpData::logomid setTo logoMid
                )
            )
        }


    private fun hashBytes(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).fold("") { str, it ->
        str + "%02x".format(it)
    }.take(15)
}
