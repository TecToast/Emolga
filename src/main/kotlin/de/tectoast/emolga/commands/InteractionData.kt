package de.tectoast.emolga.commands

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.utils.Constants
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.reply_
import dev.minn.jda.ktx.messages.send
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import net.dv8tion.jda.api.utils.messages.MessageCreateData

@OptIn(ExperimentalCoroutinesApi::class)
abstract class InteractionData(
    open val user: Long,
    open val tc: Long,
    open val gid: Long
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

    suspend fun awaitResponse() = responseDeferred.await()
    abstract fun reply(
        msg: String = "",
        ephemeral: Boolean = false,
        embed: MessageEmbed? = null,
        msgCreateData: MessageCreateData? = null
    )
    abstract fun replyModal(modal: Modal)

    abstract suspend fun replyAwait(msg: String, ephemeral: Boolean = false, action: (ReplyCallbackAction) -> Unit = {})
    abstract fun deferReply(ephemeral: Boolean = false)
    fun sendMessage(msg: String) {
        textChannel.sendMessage(msg).queue()
    }


    fun replyEphemeral(msg: String) {
        reply(msg, true)
    }
}

var redirectTestCommandLogsToChannel: MessageChannel? = null

class TestInteractionData(user: Long = Constants.FLOID, tc: Long = Constants.TEST_TCID, gid: Long = Constants.G.MY) :
    InteractionData(user, tc, gid) {
    override fun reply(msg: String, ephemeral: Boolean, embed: MessageEmbed?, msgCreateData: MessageCreateData?) {
        responseDeferred.complete(CommandResponse.from(msg, ephemeral, embed, msgCreateData))
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
) : InteractionData(e.user.idLong, e.channel!!.idLong, e.guild?.idLong ?: -1) {

    override fun reply(msg: String, ephemeral: Boolean, embed: MessageEmbed?, msgCreateData: MessageCreateData?) {
        e as IReplyCallback
        val response = CommandResponse.from(msg, ephemeral, embed, msgCreateData)
        responseDeferred.complete(response)
        if (deferred)
            response.sendInto(e.hook)
        else response.sendInto(e)
    }

    override fun replyModal(modal: Modal) {
        e as IModalCallback
        responseDeferred.complete(CommandResponse("", true))
        e.replyModal(modal).queue()
    }

    override fun deferReply(ephemeral: Boolean) {
        e as IReplyCallback
        deferred = true
        e.deferReply(ephemeral)
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

class CommandResponse(val msg: String, val ephemeral: Boolean = false, val embed: MessageEmbed? = null) {
    companion object {
        fun from(msg: String, ephemeral: Boolean, embed: MessageEmbed?, msgCreateData: MessageCreateData?) =
            msgCreateData?.let {
                CommandResponse(it.content, ephemeral, it.embeds.firstOrNull())
            } ?: CommandResponse(msg, ephemeral, embed)
    }

    fun sendInto(callback: IReplyCallback) {
        callback.reply_(msg, ephemeral = ephemeral, embeds = listOfNotNull(embed)).queue()
    }

    fun sendInto(hook: InteractionHook) {
        hook.send(msg, ephemeral = ephemeral, embeds = listOfNotNull(embed)).queue()
    }

    fun sendInto(channel: MessageChannel) {
        channel.send(msg, embeds = listOfNotNull(embed)).queue()
    }

}
