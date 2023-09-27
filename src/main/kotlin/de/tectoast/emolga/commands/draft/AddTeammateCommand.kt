package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get

class AddTeammateCommand :
    Command("addteammate", "Fügt einen Spieler zu deinem Team hinzu, falls du angemeldet bist", CommandCategory.Draft) {

    init {
        argumentTemplate = ArgumentManagerTemplate.create {
            add("user", "User", "Der Spieler, den du hinzufügen möchtest", ArgumentManagerTemplate.DiscordType.USER)
        }
        slash(true, 665600405136211989, 736555250118295622)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val lsData = db.signups.get(e.guild.idLong)
        val uid = e.author.idLong
        val data = lsData?.users?.get(uid) ?: return e.reply("Du bist nicht angemeldet!")
        val member = e.arguments.getMember("user")
        data.teammates += member.idLong
        e.reply("Du hast ${member.asMention} zu deinem Team hinzugefügt!", ephemeral = true)
        lsData.giveParticipantRole(member)
        e.jda.getTextChannelById(lsData.signupChannel)!!.editMessageById(data.signupmid!!, data.toMessage(uid, lsData))
            .queue()
        lsData.save()
    }
}
