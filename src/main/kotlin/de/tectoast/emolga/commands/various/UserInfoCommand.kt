package de.tectoast.emolga.commands.various

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.sql.managers.WarnsManager
import net.dv8tion.jda.api.EmbedBuilder
import java.awt.Color
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.*

class UserInfoCommand :
    Command("userinfo", "Zeigt die Userinfo von dir bzw. dem gepingten Spieler an", CommandCategory.Various) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "user",
                "User",
                "Der User, dessen Info du haben möchtest, oder gar nichts, wenn du deine eigene Info sehen möchtest",
                ArgumentManagerTemplate.DiscordType.USER,
                true
            )
            .setExample("!userinfo @Flo")
            .build()
    }

    override fun process(e: GuildCommandEvent) {
        val args = e.arguments!!
        val member = if (args.has("user")) args.getMember("user") else e.member
        val u = member.user
        val warncount = WarnsManager.warnCount(u.idLong, member.guild.idLong)
        val format = SimpleDateFormat()
        e.textChannel.sendMessageEmbeds(
            EmbedBuilder()
                .addField("Userinfo Command für " + u.asTag, "UserID | " + u.id, true)
                .addField(
                    "User Status",
                    if (CommandCategory.Admin.allowsMember(member)) "Admin" else if (CommandCategory.Moderator.allowsMember(
                            member
                        )
                    ) "Moderator" else "User",
                    true
                )
                .addField("Server Warns", warncount.toString(), true)
                .addField(
                    "Serverbeitritt",
                    e.guild.retrieveMember(u)
                        .complete().timeJoined.format(DateTimeFormatter.ofPattern("dd.MM.yyyy hh:mm")) + " Uhr",
                    true
                )
                .addField(
                    "Discordbeitritt",
                    u.timeCreated.format(DateTimeFormatter.ofPattern("dd.MM.yyyy hh:mm")) + " Uhr",
                    true
                )
                .addField(
                    "Boostet seit", if (member.timeBoosted == null) "Boostet nicht" else format.format(
                        Date(
                            member.timeBoosted!!.toEpochSecond()
                        )
                    ), true
                )
                .setFooter("Aufgerufen von " + e.member.user.asTag + " | " + e.member.id)
                .setColor(Color.CYAN)
                .setThumbnail(u.effectiveAvatarUrl).build()
        ).queue()
    }
}