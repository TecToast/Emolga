package de.tectoast.emolga.commands.moderator

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.Cooldown
import de.tectoast.emolga.utils.json.db
import org.litote.kmongo.and
import org.litote.kmongo.eq
import org.litote.kmongo.set
import org.litote.kmongo.setTo

object ResetCooldownCommand : Command(
    "resetcooldown",
    "Resettet den Cooldown der angegebenen Person",
    CommandCategory.Moderator,
    Constants.G.BS,
    Constants.G.ASL
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
        val gid = e.guild.idLong
        val mid = mem.idLong
        db.cooldowns.updateOne(
            and(Cooldown::guild eq gid, Cooldown::user eq mid), set(Cooldown::timestamp setTo -1)
        ).takeIf { it.modifiedCount > 0 }
            ?: return e.reply("$name hat noch nie seinen Nickname geändert!")
        e.reply("Der Cooldown von $name wurde resettet!")
    }
}
