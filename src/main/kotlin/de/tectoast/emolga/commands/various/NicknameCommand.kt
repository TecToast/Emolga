package de.tectoast.emolga.commands.various

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.jsolf.JSONObject

class NicknameCommand : Command(
    "nickname",
    "Ändert deinen Nickname (funktioniert nur 1x pro Woche)",
    CommandCategory.Various,
    Constants.BSID,
    Constants.ASLID
) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("name", "Neuer Name", "Der neue Nickname", ArgumentManagerTemplate.Text.any())
            .setExample("!nickname IchMagEmolga")
            .build()
    }

    override fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        val m = e.message!!
        val msg = m.contentDisplay
        val member = e.member
        val nickname = msg.substring(10)
        if (!e.guild.selfMember.canInteract(member)) {
            tco.sendMessage("Ich kann deinen Nickname nicht modifizieren!").queue()
            return
        }
        if (nickname.length > 32) {
            tco.sendMessage("Dieser Nickname ist zu lang! (Er darf maximal 32 Zeichen enthalten)").queue()
            return
        }
        try {
            val json = emolgaJSON
            val g = tco.guild
            if (!json.has("cooldowns")) json.put("cooldowns", JSONObject())
            if (!json.getJSONObject("cooldowns").has(g.id)) json.getJSONObject("cooldowns").put(g.id, JSONObject())
            if (json.getJSONObject("cooldowns").getJSONObject(g.id).has(member.id)) {
                val l = json.getJSONObject("cooldowns").getJSONObject(g.id).getString(member.id).toLong()
                val untilnow = System.currentTimeMillis() - l
                if (untilnow < 604800000) {
                    var delay = 604800000 - untilnow
                    val days = (delay / 86400000).toInt()
                    delay -= days * 86400000L
                    val hours = (delay / 3600000).toInt()
                    delay -= hours * 3600000L
                    val minutes = (delay / 60000).toInt()
                    tco.sendMessage(
                        """${member.asMention} Du kannst deinen Namen noch nicht wieder ändern!
Cooldown: ${days}d ${hours}h ${minutes}m"""
                    ).queue()
                    return
                }
            }
            val oldname = member.effectiveName
            member.modifyNickname(nickname).complete()
            if (g.id == "518008523653775366") EmolgaMain.emolgajda.getGuildById("518008523653775366")!!
                .getTextChannelById("728675253924003870")!!.sendMessage("$oldname hat sich in $nickname umbenannt!")
                .queue()
            tco.sendMessage(member.asMention + " Dein Nickname wurde erfolgreich geändert!").queue()
            json.getJSONObject("cooldowns").getJSONObject(g.id)
                .put(member.id, System.currentTimeMillis().toString())
            saveEmolgaJSON()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}