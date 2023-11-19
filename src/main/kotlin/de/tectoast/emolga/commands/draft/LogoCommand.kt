package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.file
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.send
import net.dv8tion.jda.api.utils.FileUpload

object LogoCommand : Command("logo", "Reicht dein Logo ein", CommandCategory.Draft) {
    init {
        argumentTemplate = ArgumentManagerTemplate.create {
            add("logo", "Logo", "Das Logo", ArgumentManagerTemplate.DiscordFile("*"))
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
        val file = try {
            logo.proxy.downloadToFile("leaguelogos/${uid}.png".file()).await()
        } catch (ex: Exception) {
            ex.printStackTrace()
            return e.hook.send("Das Logo konnte nicht heruntergeladen werden!", ephemeral = true).queue()
        }
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
