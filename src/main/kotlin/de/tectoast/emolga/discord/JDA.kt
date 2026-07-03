package de.tectoast.emolga.discord

import de.tectoast.emolga.domain.league.signup.model.form.FileSubmission
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.generics.getChannel
import kotlinx.coroutines.future.await
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageEditData
import org.koin.core.annotation.Single

fun Message.Attachment.toFileSubmission() = FileSubmission(
    fileExtension.orEmpty()
) { proxy.download().await().use { inputStream -> inputStream.readAllBytes() } }

@Single
class JDAGuildMemberRepository(private val jda: JDA) : GuildMemberRepository {
    override suspend fun hasRole(guildId: Long, memberId: Long, roleId: Long): Boolean {
        return jda.getGuildById(guildId)?.retrieveMemberById(memberId)?.await()
            ?.let { member -> member.unsortedRoles.any { it.idLong == roleId } } ?: false
    }

    private fun getGuildAndRole(guildId: Long, roleId: Long): Pair<Guild, Role>? {
        val guild = jda.getGuildById(guildId) ?: return null
        val role = guild.getRoleById(roleId) ?: return null
        return guild to role
    }

    override suspend fun addRole(guildId: Long, memberId: Long, roleId: Long) {
        val (guild, role) = getGuildAndRole(guildId, roleId) ?: return
        guild.addRoleToMember(UserSnowflake.fromId(memberId), role).queue()
    }

    override suspend fun removeRole(guildId: Long, memberId: Long, roleId: Long) {
        val (guild, role) = getGuildAndRole(guildId, roleId) ?: return
        guild.removeRoleFromMember(UserSnowflake.fromId(memberId), role).queue()
    }

    override suspend fun modifyRoles(
        guildId: Long, memberId: Long, add: List<Long>, remove: List<Long>
    ) {
        val guild = jda.getGuildById(guildId) ?: return
        guild.modifyMemberRoles(
            guild.retrieveMemberById(memberId).await(),
            add.mapNotNull { guild.getRoleById(it) },
            remove.mapNotNull { guild.getRoleById(it) }).queue()
    }
}

@Single
class JDAChannelInterface(private val jda: JDA) : ChannelInterface {
    override suspend fun sendMessage(channelId: Long, message: MessageCreateData): Long? {
        val channel = jda.getTextChannelById(channelId)
        return channel?.sendMessage(message)?.await()?.idLong
    }

    override suspend fun editMessage(
        channelId: Long, messageId: Long, message: MessageEditData
    ) {
        val channel = jda.getTextChannelById(channelId)
        channel?.editMessageById(messageId, message)?.queue()
    }

    override suspend fun deleteMessage(channelId: Long, messageId: Long) {
        val channel = jda.getTextChannelById(channelId)
        channel?.deleteMessageById(messageId)?.queue()
    }
}

@Single
class JDAVoiceStateRepository(private val jda: JDA) : VoiceStateRepository {
    override suspend fun getMembersInVoiceChannel(guildId: Long, channelId: Long): Set<Long> {
        val channel = jda.getVoiceChannelById(channelId) ?: return emptySet()
        return channel.members.mapTo(mutableSetOf()) { it.idLong }
    }

    override suspend fun kickVoiceMember(guildId: Long, memberId: Long) {
        val guild = jda.getGuildById(guildId) ?: return
        guild.kickVoiceMember(UserSnowflake.fromId(memberId)).queue()
    }
}

@Single
class JDAGuildMetaRepository(private val jda: JDA) : GuildMetaRepository {
    override suspend fun getGuildName(guildId: Long): String? {
        return jda.getGuildById(guildId)?.name
    }

    override suspend fun getGuildIcon(guildId: Long): String? {
        return jda.getGuildById(guildId)?.iconUrl
    }

    override suspend fun getGuildFromChannelId(channel: Long): Long? {
        return jda.getChannel<GuildChannel>(channel)?.guild?.idLong
    }
}

@Single
class JDAGuildChannelRepository(private val jda: JDA) : GuildChannelRepository {
    override suspend fun getChannelsOfGuild(guildId: Long): Map<String, Map<String, String>> {
        val guild = jda.getGuildById(guildId) ?: return emptyMap()
        return guild.categories.associate { cat -> cat.name to cat.textChannels.associate { it.id to it.name } }
    }
}

@Single
class JDAGuildRoleRepository(private val jda: JDA) : GuildRoleRepository {
    override suspend fun getRolesOfGuild(guildId: Long): Map<Long, String> {
        val guild = jda.getGuildById(guildId) ?: return emptyMap()
        val self = guild.selfMember
        return guild.roles.filter { !it.isPublicRole }
            .associate { it.idLong * (if (self.canInteract(it)) 1 else -1) to it.name }
    }
}

@Single
class JDAGuildInviteCreator(private val jda: JDA) : GuildInviteCreator {
    override suspend fun createOneTimeInvite(guildId: Long): String? {
        val guild = jda.getGuildById(guildId) ?: return null
        val channel = guild.defaultChannel ?: return null
        return channel.createInvite().setMaxUses(1).await().url
    }
}

@Single
class JDAChannelPermissionChecker(private val jda: JDA) : ChannelPermissionChecker {
    override suspend fun hasSelfWritePermission(channelId: Long): Boolean {
        val channel = jda.getChannel<GuildMessageChannel>(channelId) ?: return false
        val self = channel.guild.selfMember
        return (channel.isText() && channel.hasTextPermissions(self)) || (channel.isThread() && channel.hasThreadPermissions(
            self
        ))
    }

    private fun GuildMessageChannel.isText() = type == ChannelType.TEXT
    private fun GuildMessageChannel.isThread() =
        type == ChannelType.GUILD_PUBLIC_THREAD || type == ChannelType.GUILD_PRIVATE_THREAD

    private fun GuildMessageChannel.hasTextPermissions(self: Member) =
        self.hasPermission(this, Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND)

    private fun GuildMessageChannel.hasThreadPermissions(self: Member) =
        self.hasPermission(this, Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND_IN_THREADS)
}

@Single
class JDAInteractableChecker(private val jda: JDA) : InteractableChecker {
    override suspend fun canInteractWithUser(guildId: Long, userId: Long): Boolean {
        val guild = jda.getGuildById(guildId) ?: return false
        val self = guild.selfMember
        val target = guild.retrieveMemberById(userId).await()
        return self.canInteract(target)
    }
}

@Single
class JDANicknameSetter(private val jda: JDA) : NicknameSetter {
    override suspend fun setNickname(guildId: Long, memberId: Long, nickname: String) {
        val guild = jda.getGuildById(guildId) ?: return
        val member = guild.retrieveMemberById(memberId).await()
        member.modifyNickname(nickname).queue()
    }
}

@Single
class JDAChannelPresenceChecker(private val jda: JDA) : ChannelPresenceChecker {
    override suspend fun doesChannelExist(channel: Long) = jda.getTextChannelById(channel) != null
}

@Single
class JDAGeneralMessageSender(private val jda: JDA) : GeneralMessageSender {
    override fun sendToUser(id: Long, content: String) {
        jda.openPrivateChannelById(id).flatMap { pc -> pc.sendMessage(content.take(Message.MAX_CONTENT_LENGTH)) }
            .queue()
    }

    override fun sendToChannel(id: Long, content: String) {
        jda.getChannel<MessageChannel>(id)?.sendMessage(content.take(Message.MAX_CONTENT_LENGTH))?.queue()
    }
}

@Single
class JDADMSender(private val jda: JDA) : DMSender {
    override fun sendDM(userId: Long, message: MessageCreateData) {
        jda.openPrivateChannelById(userId).flatMap { it.sendMessage(message) }.queue()
    }
}

@Single
class JDADiscordUserProvider(private val jda: JDA) : DiscordUserProvider {
    override suspend fun provideUser(userId: Long): DiscordUserData {
        val user = jda.retrieveUserById(userId).await()
        return DiscordUserData(
            userId = user.idLong, displayName = user.effectiveName, avatarUrl = user.effectiveAvatarUrl
        )
    }

    override suspend fun provideMultipleUsers(guild: Long, userIds: Iterable<Long>): Map<Long, DiscordUserData> {
        val guildObj = jda.getGuildById(guild) ?: return emptyMap()
        return userIds.chunked(100).flatMap { chunk ->
            guildObj.retrieveMembersByIds(chunk).await().map {
                it.idLong to DiscordUserData(
                    userId = it.idLong, displayName = it.effectiveName, avatarUrl = it.effectiveAvatarUrl
                )
            }
        }.toMap()
    }
}

@Single
class JDAEntityValidator(private val jda: JDA) : DiscordEntityValidator {
    override suspend fun validateChannelId(channelId: Long): Boolean = jda.getTextChannelById(channelId) != null

    override suspend fun validateRoleId(roleId: Long): Boolean = jda.getRoleById(roleId) != null
}