package de.tectoast.emolga.features

import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.OneTimeCache
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.callbacks.IDeferrableCallback
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.api.utils.AttachedFile
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageEditData

abstract class InteractionData(
    open val user: Long,
    open val tc: Long,
    open val gid: Long,
    member: Member? = null,
    open val event: GenericInteractionCreateEvent? = null
) {

    val acknowledged: CompletableDeferred<Unit> = CompletableDeferred()
    val responseDeferred: CompletableDeferred<CommandResponse> = CompletableDeferred()
    var deferred = false

    val self get() = this

    val replied get() = responseDeferred.isCompleted
    val textChannel by lazy { jda.getTextChannelById(tc)!! }
    val member = OneTimeCache(member) { guild().retrieveMemberById(user).await() }
    val userObj = OneTimeCache(member?.user) { jda.retrieveUserById(user).await() }
    val guild = OneTimeCache(member?.guild) { jda.getGuildById(gid)!! }
    val message by lazy {
        (event as? GenericComponentInteractionCreateEvent)?.message ?: (event as ModalInteractionEvent).message!!
    }
    val hook by lazy { (event as IDeferrableCallback).hook }
    private var ephemeralDefault = false
    val jda: JDA by lazy { member?.jda ?: de.tectoast.emolga.bot.jda }

    suspend fun awaitResponse() = responseDeferred.await()
    abstract fun reply(
        ephemeral: Boolean = ephemeralDefault,
        msgCreateData: MessageCreateData
    )

    abstract suspend fun replyAwait(ephemeral: Boolean = ephemeralDefault, msgCreateData: MessageCreateData)
    abstract fun replyModal(modal: Modal)
    abstract fun edit(
        msgEditData: MessageEditData
    )

    fun done(ephemeral: Boolean = false) = reply("Done!", ephemeral = ephemeral)


    fun ephemeralDefault() {
        ephemeralDefault = true
    }

    fun reply(
        content: String = SendDefaults.content,
        embeds: Collection<MessageEmbed> = SendDefaults.embeds,
        components: Collection<LayoutComponent> = SendDefaults.components,
        files: Collection<FileUpload> = emptyList(),
        tts: Boolean = false,
        mentions: Mentions = Mentions.default(),
        ephemeral: Boolean = ephemeralDefault,
    ) = reply(ephemeral, MessageCreate(content, embeds, files, components, tts, mentions))

    fun edit(
        content: String? = null,
        embeds: Collection<MessageEmbed>? = null,
        components: Collection<LayoutComponent>? = null,
        attachments: Collection<AttachedFile>? = null,
        replace: Boolean = MessageEditDefaults.replace,
    ) = edit(MessageEdit(content, embeds, attachments, components, null, replace))


    suspend fun replyAwait(
        content: String = SendDefaults.content,
        embeds: Collection<MessageEmbed> = SendDefaults.embeds,
        components: Collection<LayoutComponent> = SendDefaults.components,
        files: Collection<FileUpload> = emptyList(),
        tts: Boolean = false,
        mentions: Mentions = Mentions.default(),
        ephemeral: Boolean = ephemeralDefault
    ) = replyAwait(ephemeral, MessageCreate(content, embeds, files, components, tts, mentions))

    abstract fun deferReply(ephemeral: Boolean = ephemeralDefault)
    abstract fun deferEdit()
    fun sendMessage(msg: String) {
        textChannel.sendMessage(msg).queue()
    }

    protected fun markAcknowledged() {
        acknowledged.complete(Unit)
    }


    val isNotFlo get() = user != Constants.FLOID
}

var redirectTestCommandLogsToChannel: MessageChannel? = null

@OptIn(ExperimentalCoroutinesApi::class)
class TestInteractionData(user: Long = Constants.FLOID, tc: Long = Constants.TEST_TCID, gid: Long = Constants.G.MY) :
    InteractionData(user, tc, gid) {

    init {
        redirectTestCommandLogsToChannel?.let { logsChannel ->
            responseDeferred.invokeOnCompletion {
                responseDeferred.getCompleted().sendInto(logsChannel)
            }
        }
    }

    override fun reply(ephemeral: Boolean, msgCreateData: MessageCreateData) {
        markAcknowledged()
        responseDeferred.complete(CommandResponse(ephemeral, msgCreateData))
    }

    override fun edit(msgEditData: MessageEditData) {
        markAcknowledged()
        responseDeferred.complete(CommandResponse(msgEditData = msgEditData))
    }

    override fun replyModal(modal: Modal) {
        markAcknowledged()
        responseDeferred.complete(CommandResponse())
    }

    override fun deferReply(ephemeral: Boolean) {
        markAcknowledged()
        deferred = true
    }

    override fun deferEdit() {
        markAcknowledged()
        deferred = true
    }


    override suspend fun replyAwait(ephemeral: Boolean, msgCreateData: MessageCreateData) {
        markAcknowledged()
        responseDeferred.complete(CommandResponse(ephemeral, msgCreateData))
    }
}

class RealInteractionData(
    val e: GenericInteractionCreateEvent
) : InteractionData(e.user.idLong, e.channel!!.idLong, e.guild?.idLong ?: -1, e.member, e) {

    override fun reply(ephemeral: Boolean, msgCreateData: MessageCreateData) {
        e as IReplyCallback
        val response = CommandResponse(ephemeral, msgCreateData)
        if (deferred || replied)
            response.sendInto(e.hook)
        else response.sendInto(e)
        markAcknowledged()
        responseDeferred.complete(response)
    }

    override fun edit(msgEditData: MessageEditData) {
        e as IMessageEditCallback
        val response = CommandResponse(msgEditData = msgEditData)
        if (deferred || replied)
            response.editInto(e.hook)
        else response.editInto(e)
        markAcknowledged()
        responseDeferred.complete(response)
    }

    override fun replyModal(modal: Modal) {
        e as IModalCallback
        markAcknowledged()
        responseDeferred.complete(CommandResponse())
        e.replyModal(modal).queue()
    }

    override fun deferReply(ephemeral: Boolean) {
        e as IReplyCallback
        deferred = true
        markAcknowledged()
        e.deferReply(ephemeral).queue()
    }

    override fun deferEdit() {
        e as IMessageEditCallback
        deferred = true
        markAcknowledged()
        e.deferEdit().queue()
    }

    override suspend fun replyAwait(ephemeral: Boolean, msgCreateData: MessageCreateData) {
        e as IReplyCallback
        markAcknowledged()
        responseDeferred.complete(CommandResponse(ephemeral, msgCreateData))
        e.reply(msgCreateData).setEphemeral(ephemeral).await()
    }
}

class CommandResponse(
    val ephemeral: Boolean = false,
    val msgCreateData: MessageCreateData? = null,
    val msgEditData: MessageEditData? = null
) {

    val msg: String? get() = msgCreateData?.content

    fun sendInto(callback: IReplyCallback) {
        callback.reply(msgCreateData!!).setEphemeral(ephemeral).queue()
    }

    fun sendInto(hook: InteractionHook) {
        hook.sendMessage(msgCreateData!!).setEphemeral(ephemeral).queue()
    }

    fun sendInto(channel: MessageChannel) {
        msgCreateData?.let { channel.sendMessage(it).queue() }
    }

    fun editInto(callback: IMessageEditCallback) {
        callback.editMessage(msgEditData!!).queue()
    }

    fun editInto(hook: InteractionHook) {
        hook.editOriginal(msgEditData!!).queue()
    }

}
