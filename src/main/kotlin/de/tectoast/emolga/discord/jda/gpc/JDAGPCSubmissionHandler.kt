package de.tectoast.emolga.discord.jda.gpc

import de.tectoast.emolga.domain.guildspecific.gpc.service.GPCSubmissionHandler
import de.tectoast.emolga.utils.BotConstants
import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import org.koin.core.annotation.Single


@Single
class JDAGPCSubmissionHandler(private val jda: JDA, private val botConstants: BotConstants) : GPCSubmissionHandler {
    override suspend fun handle(
        uid: Long,
        catId: Long,
        name: String,
        docUrl: String,
        metaInfos: String,
        otherInfos: String
    ): String {
        val category = jda.getCategoryById(catId)
            ?: return "Es liegt eine Fehlkonfiguration vor, melde dich bitte bei ${botConstants.botOwnerTag}!"
        val viewChannel = listOf(Permission.VIEW_CHANNEL)
        val empty = emptyList<Permission>()
        val tc = category.createTextChannel(name.take(100)).addRolePermissionOverride(
            botConstants.gpcAdminRoleId, viewChannel, empty
        ).addMemberPermissionOverride(
            uid, viewChannel, empty
        ).addMemberPermissionOverride(jda.selfUser.idLong, viewChannel, empty).addRolePermissionOverride(
            category.guild.idLong, empty, viewChannel
        ).await()
        tc.sendMessage("**${name}** (<@$uid>)\n**Doc-Link:** ${docUrl}\n\n**Infos zum Meta:**\n```${metaInfos.ifBlank { " " }}```\n**Sonstige Infos:**\n```${otherInfos.ifBlank { " " }}```")
            .await()
        return "Es wurde ein Kanal für die Registrierung deiner Liga erstellt: ${tc.asMention}"
    }
}