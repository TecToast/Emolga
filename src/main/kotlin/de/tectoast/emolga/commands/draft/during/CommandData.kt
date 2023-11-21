package de.tectoast.emolga.commands.draft.during

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction

abstract class CommandData(
    open val user: Long,
    open val tc: Long,
    open val gid: Long
) {
    var reply: String? = null

    val acknowledged get() = reply != null
    val textChannel by lazy { jda.getTextChannelById(tc)!! }

    abstract fun reply(msg: String, ephemeral: Boolean = false)
    abstract suspend fun replyAwait(msg: String, ephemeral: Boolean = false, action: (ReplyCallbackAction) -> Unit = {})
    fun sendMessage(msg: String) {
        textChannel.sendMessage(msg).queue()
    }


    fun replyEphemeral(msg: String) {
        reply(msg, true)
    }
}

class TestCommandData(user: Long, tc: Long, gid: Long) : CommandData(user, tc, gid) {
    override fun reply(msg: String, ephemeral: Boolean) {
        reply = msg
    }

    override suspend fun replyAwait(msg: String, ephemeral: Boolean, action: (ReplyCallbackAction) -> Unit) {
        reply = msg
    }
}

class RealCommandData(
    val e: GuildCommandEvent
) : CommandData(e.author.idLong, e.channel.idLong, e.guild.idLong) {

    override fun reply(msg: String, ephemeral: Boolean) {
        reply = msg
        e.reply_(msg, ephemeral = ephemeral)
    }

    override suspend fun replyAwait(msg: String, ephemeral: Boolean, action: (ReplyCallbackAction) -> Unit) {
        reply = msg
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
