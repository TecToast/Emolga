package de.tectoast.emolga.commands.draft.during

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction

abstract class DraftCommandData(
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

class TestDraftCommandData(user: Long, tc: Long, gid: Long) : DraftCommandData(user, tc, gid) {
    override fun reply(msg: String, ephemeral: Boolean) {
        TODO("Not yet implemented")

    }

    override suspend fun replyAwait(msg: String, ephemeral: Boolean, action: (ReplyCallbackAction) -> Unit) {
        TODO("Not yet implemented")
    }
}

class RealDraftCommandData(
    e: GuildCommandEvent
) : DraftCommandData(e.author.idLong, e.channel.idLong, e.guild.idLong) {

    override fun reply(msg: String, ephemeral: Boolean) {
        TODO("Not yet implemented")
    }

    override suspend fun replyAwait(msg: String, ephemeral: Boolean, action: (ReplyCallbackAction) -> Unit) {
        TODO("Not yet implemented")
    }

}

abstract class DraftCommand<T : SpecifiedDraftCommandData>(
    name: String,
    help: String,
    category: CommandCategory = CommandCategory.Draft
) : Command(name, help, category) {
    init {
        slash(true, *draftGuilds)
    }

    override suspend fun process(e: GuildCommandEvent) =
        with(RealDraftCommandData(e)) { exec(fromGuildCommandEvent(e)) }

    abstract fun fromGuildCommandEvent(e: GuildCommandEvent): T
    context (DraftCommandData)
    abstract suspend fun exec(e: T)
}

interface SpecifiedDraftCommandData
object NoSpecifiedDraftCommandData : SpecifiedDraftCommandData
