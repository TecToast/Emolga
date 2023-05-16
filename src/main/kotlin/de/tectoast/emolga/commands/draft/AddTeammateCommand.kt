package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.saveEmolgaJSON
import de.tectoast.emolga.utils.json.Emolga

class AddTeammateCommand :
    Command("addteammate", "Fügt einen Spieler zu deinem Team hinzu, falls du angemeldet bist", CommandCategory.Draft) {

    init {
        argumentTemplate = ArgumentManagerTemplate.create {
            add("user", "User", "Der Spieler, den du hinzufügen möchtest", ArgumentManagerTemplate.DiscordType.USER)
        }
        slash(true, 665600405136211989)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val league = Emolga.get.signups[e.guild.idLong]
        val uid = e.author.idLong
        val data = league?.users?.get(uid) ?: return e.reply("Du bist nicht angemeldet!")
        val member = e.arguments.getMember("user")
        data.teammates += member.idLong
        e.reply("Du hast ${member.asMention} zu deinem Team hinzugefügt!", ephemeral = true)
        e.jda.getTextChannelById(league.signupChannel)!!.editMessageById(data.signupmid!!, data.toMessage(uid)).queue()
        saveEmolgaJSON()
    }
}
