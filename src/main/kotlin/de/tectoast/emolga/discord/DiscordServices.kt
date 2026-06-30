package de.tectoast.emolga.discord

import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.utils.t
import de.tectoast.k18n.generated.K18nMessage
import dev.minn.jda.ktx.messages.MessageCreate
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageEditData

fun interface MessageSender {
    suspend fun sendMessage(message: MessageCreateData)
}

fun interface K18nMessageSender {
    suspend fun sendMessage(message: K18nMessage)
}

suspend fun MessageSender.sendMessage(msg: String) = sendMessage(MessageCreateData.fromContent(msg))

interface ChannelInterface {
    suspend fun sendMessage(channelId: Long, message: MessageCreateData): Long?
    suspend fun editMessage(channelId: Long, messageId: Long, message: MessageEditData)
    suspend fun deleteMessage(channelId: Long, messageId: Long)
}

fun ChannelInterface.createSingleChannel(channelId: Long) = MessageSender { sendMessage(channelId, it) }

context(iData: InteractionData)
fun ChannelInterface.createK18nMessageSender(channelId: Long) = K18nMessageSender { sendMessage(channelId, it.t()) }

suspend fun ChannelInterface.sendMessage(channelId: Long, message: String, mentionUsers: List<Long>? = null): Long? {
    return sendMessage(channelId, MessageCreate {
        content = message
        mentionUsers?.let {
            mentions {
                users += it
            }
        }
    })
}

suspend fun ChannelInterface.editMessage(channelId: Long, messageId: Long, message: String) {
    editMessage(channelId, messageId, MessageEditData.fromContent(message))
}

interface GuildMemberRepository {
    suspend fun hasRole(guildId: Long, memberId: Long, roleId: Long): Boolean
    suspend fun addRole(guildId: Long, memberId: Long, roleId: Long)
    suspend fun removeRole(guildId: Long, memberId: Long, roleId: Long)
    suspend fun modifyRoles(guildId: Long, memberId: Long, add: List<Long>, remove: List<Long>)
}


interface VoiceStateRepository {
    suspend fun getMembersInVoiceChannel(guildId: Long, channelId: Long): Set<Long>
    suspend fun kickVoiceMember(guildId: Long, memberId: Long)
}


interface GuildMetaRepository {
    suspend fun getGuildName(guildId: Long): String?
    suspend fun getGuildIcon(guildId: Long): String?

    suspend fun getGuildFromChannelId(channel: Long): Long?
}

interface GuildChannelRepository {
    suspend fun getChannelsOfGuild(guildId: Long): Map<String, Map<String, String>>
}

interface GuildRoleRepository {
    /**
     * Returns a map of role ids (negative if not interactable) to role names.
     */
    suspend fun getRolesOfGuild(guildId: Long): Map<Long, String>
}


interface GuildInviteCreator {
    suspend fun createOneTimeInvite(guildId: Long): String?
}


interface ChannelPermissionChecker {
    suspend fun hasSelfWritePermission(channelId: Long): Boolean
}

interface InteractableChecker {
    suspend fun canInteractWithUser(guildId: Long, userId: Long): Boolean
}

interface NicknameSetter {
    suspend fun setNickname(guildId: Long, memberId: Long, nickname: String)
}


interface ChannelPresenceChecker {
    suspend fun doesChannelExist(channel: Long): Boolean
}


interface GeneralMessageSender {
    fun sendToUser(id: Long, content: String)
    fun sendToChannel(id: Long, content: String)
}


interface DMSender {
    fun sendDM(userId: Long, message: MessageCreateData)
}

fun DMSender.sendDM(userId: Long, content: String) = sendDM(userId, MessageCreateData.fromContent(content))


interface DiscordUserProvider {
    suspend fun provideUser(userId: Long): DiscordUserData
    suspend fun provideMultipleUsers(guild: Long, userIds: Iterable<Long>): Map<Long, DiscordUserData>
}

data class DiscordUserData(
    val userId: Long,
    val displayName: String,
    val avatarUrl: String
)

interface DiscordEntityValidator {
    suspend fun validateChannelId(channelId: Long): Boolean
    suspend fun validateRoleId(roleId: Long): Boolean
}