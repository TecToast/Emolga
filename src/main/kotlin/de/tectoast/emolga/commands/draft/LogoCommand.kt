package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.send
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.utils.FileUpload
import java.io.File
import java.security.MessageDigest

object LogoCommand : Command("logo", "Reicht dein Logo ein", CommandCategory.Draft) {
    init {
        argumentTemplate = ArgumentManagerTemplate.create {
            add(
                "logo",
                "Logo",
                "Das Logo",
                ArgumentManagerTemplate.DiscordFile.of("png", "PNG", "jpg", "JPG", "jpeg", "JPEG")
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
        val file = withContext(Dispatchers.IO) {
            val bytes = try {
                logo.proxy.download().await().readAllBytes()
            } catch (ex: Exception) {
                ex.printStackTrace()
                e.hook.send("Das Logo konnte nicht heruntergeladen werden!", ephemeral = true).queue()
                return@withContext null
            }
            val filename = MessageDigest.getInstance("SHA-256").digest(bytes).fold("") { str, it ->
                str + "%02x".format(it)
            }.take(15)
            File("leaguelogos/${filename}.${logo.fileExtension}").apply { writeBytes(bytes) }
        } ?: return
        e.hook.send("Das Logo wurde erfolgreich hochgeladen!", ephemeral = true).queue()
        val signUpData = ligaStartData.users[uid]!!
        val tc = e.jda.getTextChannelById(ligaStartData.logoChannel)!!
        signUpData.logomid?.let {
            tc.deleteMessageById(it).queue(null) {}
        }
        signUpData.logoUrl =
            tc.sendMessage(
                "**Logo von ${
                    listOf(
                        uid,
                        *signUpData.teammates.toTypedArray()
                    ).joinToString(" & ") { "<@$it>" }
                } (${signUpData.teamname}):**"
            )
                .addFiles(FileUpload.fromData(file)).await()
                .also { signUpData.logomid = it.idLong }.attachments[0].url
        ligaStartData.save()

    }
}
