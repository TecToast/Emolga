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
        slash(true, Constants.G.FLP)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val ligaStartData = Emolga.get.signups[e.guild.idLong]!!
        val uid = e.author.idLong
        if (uid !in ligaStartData.users) {
            e.reply("Du bist nicht angemeldet!", ephermal = true)
            return
        }
        e.deferReply(true)
        val logo = e.arguments.getAttachment("logo")
        val file = logo.proxy.downloadToFile("leaguelogos/${e.author.id}.png".file()).await()
        e.reply("Dein Logo wurde erfolgreich hochgeladen!", ephermal = true)
        val signUpData = ligaStartData.users[uid]!!
        signUpData.logoUrl = e.jda.getTextChannelById(ligaStartData.logoChannel)!!
            .sendMessage("**Logo von <@$uid> (${signUpData.teamname}):**").addFiles(FileUpload.fromData(file))
            .await().attachments[0].url
        saveEmolgaJSON()
    }
}
