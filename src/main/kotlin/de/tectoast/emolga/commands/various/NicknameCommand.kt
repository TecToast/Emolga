package de.tectoast.emolga.commands.various

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.saveEmolgaJSON
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.Emolga

class NicknameCommand : Command(
    "nickname",
    "Ändert deinen Nickname (funktioniert nur 1x pro Woche)",
    CommandCategory.Various,
    Constants.G.BS,
    Constants.G.ASL
) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("name", "Neuer Name", "Der neue Nickname", ArgumentManagerTemplate.Text.any())
            .setExample("!nickname IchMagEmolga").build()
        slash(true, Constants.G.ASL)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val member = e.member
        val nickname = e.arguments.getText("name")
        if (!e.guild.selfMember.canInteract(member)) {
            e.reply("Ich kann deinen Nickname nicht modifizieren!")
            return
        }
        if (nickname.length > 32) {
            e.reply("Dieser Nickname ist zu lang! (Er darf maximal 32 Zeichen enthalten)")
            return
        }
        val g = e.guild

        //secondsToTime()
        val map = Emolga.get.cooldowns.getOrPut(g.idLong) { mutableMapOf() }
        map[member.id]?.run {
            val expiresIn = this - System.currentTimeMillis()
            if (expiresIn <= 0) return@run
            e.reply(
                "${member.asMention} Du kannst deinen Namen noch nicht wieder ändern!\nCooldown: ${
                    secondsToTime(expiresIn / 1000)
                }"
            )
            return
        }
        //val oldname = member.effectiveName
        member.modifyNickname(nickname).queue {
            /*if (g.idLong == Constants.G.ASL) g.getTextChannelById("728675253924003870")!!
                .sendMessage("$oldname hat sich in $nickname umbenannt!").queue()*/
            e.reply(member.asMention + " Dein Nickname wurde erfolgreich geändert!")
            map[member.id] = System.currentTimeMillis() + 604800000
            saveEmolgaJSON()
        }
    }
}
