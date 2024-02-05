package de.tectoast.emolga.commands

import de.tectoast.emolga.utils.Constants
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import net.dv8tion.jda.api.utils.AttachedFile
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageEditData

@OptIn(ExperimentalCoroutinesApi::class)
abstract class InteractionData(
    open val user: Long,
    open val tc: Long,
    open val gid: Long,
    member: Member? = null,
    open val event: GenericInteractionCreateEvent? = null
) {

    val responseDeferred: CompletableDeferred<CommandResponse> = CompletableDeferred()
    var deferred = false

    init {
        redirectTestCommandLogsToChannel?.let { logsChannel ->
            responseDeferred.invokeOnCompletion {
                responseDeferred.getCompleted().sendInto(logsChannel)
            }
        }
    }

    val self get() = this

    val acknowledged get() = responseDeferred.isCompleted
    val textChannel by lazy { jda.getTextChannelById(tc)!! }
    private var _member: Member? = member
    private var _user: User? = member?.user
    private var _guild: Guild? = member?.guild
    var ephemeralDefault = false
    val jda: JDA = member?.jda ?: de.tectoast.emolga.bot.jda
    suspend fun member() = _member ?: run {
        guild().retrieveMemberById(user).await().also { _member = it }!!
    }

    suspend fun user() = _user ?: run {
        jda.retrieveUserById(user).await().also { _user = it }!!
    }

    fun guild() = _guild ?: run {
        jda.getGuildById(gid)!!.also { _guild = it }
    }


    /**
     * Executes the given handler if the event exists and is of the given type
     * @param T The type of the event
     * @param handler The handler to execute
     */
    inline fun <reified T : GenericInteractionCreateEvent> event(handler: T.() -> Unit) {
        (event as? T)?.handler()
    }

    inline fun buttonEvent(handler: ButtonInteractionEvent.() -> Unit) {
        event<ButtonInteractionEvent> { handler() }
    }

    inline fun modalEvent(handler: ModalInteractionEvent.() -> Unit) {
        event<ModalInteractionEvent> { handler() }
    }


    suspend fun awaitResponse() = responseDeferred.await()
    abstract fun reply(
        ephemeral: Boolean = ephemeralDefault,
        msgCreateData: MessageCreateData
    )

    fun done(ephemeral: Boolean = false) = reply("Done!", ephemeral = ephemeral)

    abstract fun edit(
        msgEditData: MessageEditData
    )

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

    abstract fun replyModal(modal: Modal)

    abstract suspend fun replyAwait(msg: String, ephemeral: Boolean = false, action: (ReplyCallbackAction) -> Unit = {})
    abstract fun deferReply(ephemeral: Boolean = ephemeralDefault)
    fun sendMessage(msg: String) {
        textChannel.sendMessage(msg).queue()
    }


    val isNotFlo get() = user != Constants.FLOID
}

var redirectTestCommandLogsToChannel: MessageChannel? = null

class TestInteractionData(user: Long = Constants.FLOID, tc: Long = Constants.TEST_TCID, gid: Long = Constants.G.MY) :
    InteractionData(user, tc, gid) {
    override fun reply(ephemeral: Boolean, msgCreateData: MessageCreateData) {
        responseDeferred.complete(CommandResponse.from(ephemeral, msgCreateData))
    }

    override fun edit(msgEditData: MessageEditData) {
        responseDeferred.complete(CommandResponse.from(msgEditData))
    }

    override fun replyModal(modal: Modal) {
        responseDeferred.complete(CommandResponse("", true))
    }

    override fun deferReply(ephemeral: Boolean) {
        deferred = true
    }

    override suspend fun replyAwait(msg: String, ephemeral: Boolean, action: (ReplyCallbackAction) -> Unit) {
        responseDeferred.complete(CommandResponse(msg, ephemeral))
    }
}

class RealInteractionData(
    val e: GenericInteractionCreateEvent
) : InteractionData(e.user.idLong, e.channel!!.idLong, e.guild?.idLong ?: -1, e.member, e) {

    override fun reply(ephemeral: Boolean, msgCreateData: MessageCreateData) {
        e as IReplyCallback
        val response = CommandResponse.from(ephemeral, msgCreateData)
        if (deferred || acknowledged)
            response.sendInto(e.hook)
        else response.sendInto(e)
        responseDeferred.complete(response)
    }

    override fun edit(msgEditData: MessageEditData) {
        e as IMessageEditCallback
        val response = CommandResponse.from(msgEditData)
        responseDeferred.complete(response)
        e.editMessage(msgEditData).queue()
    }

    override fun replyModal(modal: Modal) {
        e as IModalCallback
        responseDeferred.complete(CommandResponse("", true))
        e.replyModal(modal).queue()
    }

    override fun deferReply(ephemeral: Boolean) {
        e as IReplyCallback
        deferred = true
        e.deferReply(ephemeral).queue()
    }

    override suspend fun replyAwait(msg: String, ephemeral: Boolean, action: (ReplyCallbackAction) -> Unit) {
        e as IReplyCallback
        responseDeferred.complete(CommandResponse(msg, ephemeral))
        e.reply_(msg, ephemeral = ephemeral).apply(action).await()
    }

}

abstract class TestableCommand<T : CommandArgs>(
    name: String,
    help: String,
    category: CommandCategory = CommandCategory.Draft
) : Command(name, help, category) {
    init {
        slash(true, *draftGuilds)
    }

    override suspend fun process(e: GuildCommandEvent) {} //=
    //with(RealInteractionData(e)) { exec(fromGuildCommandEvent(e)) }

    abstract fun fromGuildCommandEvent(e: GuildCommandEvent): T
    context (InteractionData)
    abstract suspend fun exec(e: T)
}

interface CommandArgs
object NoCommandArgs : CommandArgs

class CommandResponse(
    val msg: String,
    val ephemeral: Boolean = false,
    val embeds: List<MessageEmbed>? = null,
    val components: List<LayoutComponent>? = null
) {
    companion object {
        fun from(ephemeral: Boolean, data: MessageCreateData) =
            CommandResponse(data.content, ephemeral, data.embeds, data.components)

        fun from(data: MessageEditData) =
            CommandResponse(data.content, false, data.embeds, data.components)
    }

    fun sendInto(callback: IReplyCallback) {
        callback.reply_(msg, ephemeral = ephemeral, embeds = embeds.orEmpty()).queue()
    }

    fun sendInto(hook: InteractionHook) {
        hook.send(msg, ephemeral = ephemeral, embeds = embeds.orEmpty()).queue()
    }

    fun sendInto(channel: MessageChannel) {
        channel.send(msg, embeds = embeds.orEmpty()).queue()
    }

}
