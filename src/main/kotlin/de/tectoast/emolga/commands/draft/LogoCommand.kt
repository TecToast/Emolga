package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.Google
import de.tectoast.emolga.utils.json.*
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.send
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.utils.FileUpload
import org.litote.kmongo.*
import java.security.MessageDigest

object LogoCommand : Command("logo", "Reicht dein Logo ein", CommandCategory.Draft) {
    init {
        argumentTemplate = ArgumentManagerTemplate.create {
            add(
                "logo",
                "Logo",
                "Das Logo",
                ArgumentManagerTemplate.DiscordFile.of()
            )
        }
        slash(
            true,
            Constants.G.FLP,
            Constants.G.ASL,
            665600405136211989,
            Constants.G.WFS,
            Constants.G.ADK,
            Constants.G.NDS
        )
    }

    override suspend fun process(e: GuildCommandEvent) {
        insertLogo(e, e.author.idLong)
    }

    private val allowedFileFormats = setOf("png", "jpg", "jpeg")

    suspend fun insertLogo(e: GuildCommandEvent, uid: Long) {
        e.deferReply(ephermal = true)
        val ligaStartData = db.signups.get(e.guild.idLong)!!
        if (uid !in ligaStartData.users) {
            return e.hook.send("Du bist nicht angemeldet!", ephemeral = true).queue()
        }
        if (ligaStartData.noTeam) {
            return e.hook.send("In dieser Liga gibt es keine eigenen Teams!", ephemeral = true).queue()
        }
        val logo = e.arguments.getAttachment("logo")
        val fileExtension = logo.fileExtension?.lowercase()?.takeIf { it in allowedFileFormats }
            ?: return e.hook.send("Das Logo muss eine Bilddatei sein!", ephemeral = true).queue()
        val (bytes, checksum) = withContext(Dispatchers.IO) {
            val bytes = try {
                logo.proxy.download().await().readAllBytes()
            } catch (ex: Exception) {
                ex.printStackTrace()
                e.hook.send("Das Logo konnte nicht heruntergeladen werden!", ephemeral = true).queue()
                return@withContext null
            }
            if (bytes.size > 1024 * 1024 * 3) {
                e.hook.send("Das Logo darf nicht größer als 3MB sein!", ephemeral = true).queue()
                return@withContext null
            }
            val checksum = hashBytes(bytes)
            bytes to checksum
        } ?: return
        e.hook.send("Das Logo wurde erfolgreich hochgeladen!", ephemeral = true).queue()
        val fileName = "$checksum.$fileExtension"
        val signUpData = ligaStartData.users[uid]!!
        val tc = e.jda.getTextChannelById(ligaStartData.logoChannel)!!
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
        uploadToCloud(bytes, fileExtension, e.guild.idLong, uid, logoMid, checksum, fileName)
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
