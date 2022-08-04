package de.tectoast.emolga.commands.various

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.embedColor
import de.tectoast.emolga.utils.sql.managers.WarnsManager
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.Embed
import net.dv8tion.jda.api.Permission
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class UserInfoCommand :
    Command("userinfo", "Zeigt die Userinfo von dir bzw. dem gepingten Spieler an", CommandCategory.Various) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder().add(
            "user",
            "User",
            "Der User, dessen Info du haben möchtest, oder gar nichts, wenn du deine eigene Info sehen möchtest",
            ArgumentManagerTemplate.DiscordType.USER,
            true
        ).setExample("!userinfo @Flo").build()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val args = e.arguments
        val member = if (args.has("user")) args.getMember("user") else e.member
        val u = member.user
        val warncount = WarnsManager.warnCount(u.idLong, member.guild.idLong)
        val format = SimpleDateFormat()
        e.reply(Embed {
            field("Userinfo Command für " + u.asTag, "UserID | " + u.id, true)
            field(
                "User Status",

                if (member.hasPermission(Permission.ADMINISTRATOR)) "Admin" else if (CommandCategory.Moderator.allowsMember(
                        member
                    )
                ) "Moderator" else "User",
                true
            )
            field("Server Warns", warncount.toString(), true)
            field(
                "Serverbeitritt",
                e.guild.retrieveMember(u)
                    .await().timeJoined.atZoneSameInstant(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy hh:mm")) + " Uhr",
                true
            )
            field(
                "Discordbeitritt",
                u.timeCreated.format(DateTimeFormatter.ofPattern("dd.MM.yyyy hh:mm")) + " Uhr",
                true
            )
            field(
                "Boostet seit", if (member.timeBoosted == null) "Boostet nicht" else format.format(
                    Date(
                        member.timeBoosted!!.toEpochSecond()
                    )
                ), true
            )
            footer("Aufgerufen von " + e.member.user.asTag + " | " + e.member.id)
            color = embedColor
            thumbnail = u.effectiveAvatarUrl
        })
    }
}