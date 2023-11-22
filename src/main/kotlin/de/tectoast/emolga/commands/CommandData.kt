package de.tectoast.emolga.commands

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.utils.Constants
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.reply_
import dev.minn.jda.ktx.messages.send
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import net.dv8tion.jda.api.utils.messages.MessageCreateData

@OptIn(ExperimentalCoroutinesApi::class)
abstract class CommandData(
    open val user: Long,
    open val tc: Long,
    open val gid: Long
) {

    val response get() = runBlocking { responseDeferred.await() }
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

    abstract fun reply(
        msg: String = "",
        ephemeral: Boolean = false,
        embed: MessageEmbed? = null,
        msgCreateData: MessageCreateData? = null
    )

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

class TestCommandData(user: Long = Constants.FLOID, tc: Long = Constants.TEST_TCID, gid: Long = Constants.G.MY) :
    CommandData(user, tc, gid) {
    override fun reply(msg: String, ephemeral: Boolean, embed: MessageEmbed?, msgCreateData: MessageCreateData?) {
        responseDeferred.complete(CommandResponse.from(msg, ephemeral, embed, msgCreateData))
    }

    override fun deferReply(ephemeral: Boolean) {
        deferred = true
    }

    override suspend fun replyAwait(msg: String, ephemeral: Boolean, action: (ReplyCallbackAction) -> Unit) {
        responseDeferred.complete(CommandResponse(msg, ephemeral))
    }
}

class RealCommandData(
    val e: GuildCommandEvent
) : CommandData(e.author.idLong, e.channel.idLong, e.guild.idLong) {

    override fun reply(msg: String, ephemeral: Boolean, embed: MessageEmbed?, msgCreateData: MessageCreateData?) {
        val response = CommandResponse.from(msg, ephemeral, embed, msgCreateData)
        responseDeferred.complete(response)
        if (deferred)
            response.sendInto(e.slashCommandEvent!!.hook)
        else response.sendInto(e.slashCommandEvent!!)
    }

    override fun deferReply(ephemeral: Boolean) {
        deferred = true
        e.deferReply(ephemeral)
    }

    override suspend fun replyAwait(msg: String, ephemeral: Boolean, action: (ReplyCallbackAction) -> Unit) {
        responseDeferred.complete(CommandResponse(msg, ephemeral))
        e.slashCommandEvent!!.reply_(msg, ephemeral = ephemeral).apply(action).await()
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

    override suspend fun process(e: GuildCommandEvent) =
        with(RealCommandData(e)) { exec(fromGuildCommandEvent(e)) }

    abstract fun fromGuildCommandEvent(e: GuildCommandEvent): T
    context (CommandData)
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
