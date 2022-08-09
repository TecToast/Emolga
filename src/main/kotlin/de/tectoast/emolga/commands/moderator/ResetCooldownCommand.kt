package de.tectoast.emolga.commands.moderator

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.saveEmolgaJSON
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.Emolga

class ResetCooldownCommand : Command(
    "resetcooldown",
    "Resettet den Cooldown der angegebenen Person",
    CommandCategory.Moderator,
    Constants.BSID,
    Constants.ASLID
) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "user",
                "User",
                "Der User, dessen Cooldown resettet werden soll",
                ArgumentManagerTemplate.DiscordType.USER
            )
            .setExample("!resetcooldown @CoolerUser")
            .build()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val mem = e.arguments.getMember("user")
        val name = "**" + mem.effectiveName + "**"
        val c = Emolga.get.cooldowns
        val gid = e.guild.idLong
        if (gid !in c) {
            e.reply("Auf diesem Server hat noch nie jemand seinen Namen geändert!")
            return
        }
        val mid = mem.id
        val o = c[gid]!!
        if (mid !in o) {
            e.reply("$name hat noch nie seinen Nickname geändert!")
            return
        }
        val l = o[mid]!!
        val untilnow = System.currentTimeMillis() - l
        if (untilnow >= 604800000) {
            e.reply("$name darf seinen Namen bereits wieder ändern!")
            return
        }
        o[mid] = -1
        e.reply("Der Cooldown von $name wurde resettet!")
        saveEmolgaJSON()
    }
}