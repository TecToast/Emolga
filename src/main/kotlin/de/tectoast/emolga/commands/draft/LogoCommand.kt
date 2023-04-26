package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.*
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.Emolga
import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.utils.FileUpload

class LogoCommand : Command("logo", "Reicht dein Logo ein", CommandCategory.Draft) {
    init {
        argumentTemplate = ArgumentManagerTemplate.create {
            add("logo", "Logo", "Das Logo", ArgumentManagerTemplate.DiscordFile("*"))
        }
        slash(true, Constants.G.FLP, Constants.G.ASL)
    }

    override suspend fun process(e: GuildCommandEvent) {
        insertLogo(e, e.author.idLong)
    }

    companion object {
        suspend fun insertLogo(e: GuildCommandEvent, uid: Long) {
            val ligaStartData = Emolga.get.signups[e.guild.idLong]!!
            if (uid !in ligaStartData.users) {
                e.reply("Du bist nicht angemeldet!", ephemeral = true)
                return
            }
            e.deferReply(true)
            val logo = e.arguments.getAttachment("logo")
            val file = logo.proxy.downloadToFile("leaguelogos/${uid}.png".file()).await()
            e.reply("Das Logo wurde erfolgreich hochgeladen!", ephemeral = true)
            val signUpData = ligaStartData.users[uid]!!
            val tc = e.jda.getTextChannelById(ligaStartData.logoChannel)!!
            signUpData.logomid?.let {
                tc.deleteMessageById(it).queue()
            }
            signUpData.logoUrl = tc
                .sendMessage("**Logo von <@$uid> (${signUpData.teamname}):**").addFiles(FileUpload.fromData(file))
                .await().also { signUpData.logomid = it.idLong }.attachments[0].url
            saveEmolgaJSON()
        }
    }
}
