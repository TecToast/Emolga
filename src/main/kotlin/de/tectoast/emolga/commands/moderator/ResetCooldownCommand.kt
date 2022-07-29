package de.tectoast.emolga.commands.moderator

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants

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

    override fun process(e: GuildCommandEvent) {
        val mem = e.arguments.getMember("user")
        val name = "**" + mem.effectiveName + "**"
        val c = emolgaJSON.getJSONObject("cooldowns")
        val gid = e.guild.id
        if (!c.has(gid)) {
            e.reply("Auf diesem Server hat noch nie jemand seinen Namen geändert!")
            return
        }
        val mid = mem.id
        val o = c.getJSONObject(gid)
        if (!o.has(mid)) {
            e.reply("$name hat noch nie seinen Nickname geändert!")
            return
        }
        val l = o.getString(mid).toLong()
        val untilnow = System.currentTimeMillis() - l
        if (untilnow >= 604800000) {
            e.reply("$name darf seinen Namen bereits wieder ändern!")
            return
        }
        o.put(mid, "-1")
        e.reply("Der Cooldown von $name wurde resettet!")
        saveEmolgaJSON()
    }
}