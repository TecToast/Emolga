package de.tectoast.emolga.commands

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.utils.Constants
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.reply_
import dev.minn.jda.ktx.messages.send
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import net.dv8tion.jda.api.utils.messages.MessageCreateData

abstract class CommandData(
    open val user: Long,
    open val tc: Long,
    open val gid: Long
) {
    lateinit var response: CommandResponse
    var deferred = false

    val self get() = this

    val acknowledged get() = ::response.isInitialized
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

class TestCommandData(user: Long = Constants.FLOID, tc: Long = Constants.TEST_TCID, gid: Long = Constants.G.MY) :
    CommandData(user, tc, gid) {
    override fun reply(msg: String, ephemeral: Boolean, embed: MessageEmbed?, msgCreateData: MessageCreateData?) {
        response = CommandResponse.from(msg, ephemeral, embed, msgCreateData)
    }

    override fun deferReply(ephemeral: Boolean) {
        deferred = true
    }

    override suspend fun replyAwait(msg: String, ephemeral: Boolean, action: (ReplyCallbackAction) -> Unit) {
        response = CommandResponse(msg, ephemeral)
    }
}

class RealCommandData(
    val e: GuildCommandEvent
) : CommandData(e.author.idLong, e.channel.idLong, e.guild.idLong) {

    override fun reply(msg: String, ephemeral: Boolean, embed: MessageEmbed?, msgCreateData: MessageCreateData?) {
        response = CommandResponse.from(msg, ephemeral, embed, msgCreateData)
        if (deferred) {
            val hook = e.slashCommandEvent?.hook
            msgCreateData?.let { hook?.sendMessage(it)?.queue() } ?: hook?.send(
                content = msg,
                ephemeral = ephemeral,
                embeds = listOfNotNull(embed)
            )?.queue()
        } else {
            msgCreateData?.let { e.slashCommandEvent?.reply(it)?.queue() } ?: e.reply_(
                msg,
                ephemeral = ephemeral,
                embeds = listOfNotNull(embed)
            )
        }
    }

    override fun deferReply(ephemeral: Boolean) {
        deferred = true
        e.deferReply(ephemeral)
    }

    override suspend fun replyAwait(msg: String, ephemeral: Boolean, action: (ReplyCallbackAction) -> Unit) {
        response = CommandResponse(msg, ephemeral)
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
}
