package de.tectoast.emolga.commands

import de.tectoast.emolga.utils.Constants
import dev.minn.jda.ktx.messages.Mentions
import dev.minn.jda.ktx.messages.SendDefaults
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import net.dv8tion.jda.api.utils.FileUpload
import org.slf4j.LoggerFactory
import java.util.function.Consumer

abstract class GenericCommandEvent {
    val message: Message?
    val author: User
    val msg: String?
    val channel: MessageChannel
    val jda: JDA
    val args: MutableList<String>
    private val mentionedChannels: List<TextChannel?>
    private val mentionedMembers: List<Member?>
    private val mentionedRoles: List<Role>
    private val argsLength: Int
    val slashCommandEvent: SlashCommandInteractionEvent?
    val hook: InteractionHook get() = slashCommandEvent!!.hook

    constructor(message: Message) {
        this.message = message
        author = message.author
        msg = message.contentDisplay
        channel = message.channel
        jda = message.jda
        val mentions = this.message.mentions
        mentionedChannels = mentions.getChannels(TextChannel::class.java)
        mentionedMembers = mentions.members
        mentionedRoles = mentions.roles
        args = Command.WHITESPACES_SPLITTER.split(msg).toMutableList()
        args.removeAt(0)
        argsLength = args.size
        slashCommandEvent = null
    }

    constructor(e: SlashCommandInteractionEvent) {
        message = null
        author = e.user
        msg = null
        channel = e.channel
        jda = e.jda
        args = e.options.map { it.asString }.toMutableList()
        mentionedChannels = e.options.mapNotNull { it.runCatching { asChannel.asTextChannel() }.getOrNull() }
        mentionedMembers = e.options.filter { it.type == OptionType.USER }.map { it.asMember }
        mentionedRoles = e.options.filter { it.type == OptionType.ROLE }.map { it.asRole }
        argsLength = args.size
        slashCommandEvent = e
    }

    fun getArg(i: Int): String {
        return if (hasArg(i)) args[i] else ""
    }

    private fun hasArg(i: Int): Boolean {
        return i < argsLength
    }


    fun reply(msg: String, ephemeral: Boolean = false) {
        if (msg.isEmpty()) return
        if (slashCommandEvent != null) {
            if (slashCommandEvent.isAcknowledged) slashCommandEvent.hook.sendMessage(msg).setEphemeral(ephemeral)
                .queue() else
                slashCommandEvent.reply(msg).setEphemeral(ephemeral)
                    .queue()
        } else channel.sendMessage(msg).queue()
    }

    fun reply(
        msg: String,
        ma: Consumer<MessageCreateAction>? = null,
        ra: Consumer<ReplyCallbackAction>? = null,
        m: Consumer<Message>? = null,
        ih: Consumer<InteractionHook>? = null
    ) {
        if (slashCommandEvent != null) {
            val reply = slashCommandEvent.reply(msg)
            ra?.accept(reply)
            reply.queue(ih)
        } else {
            val ac = channel.sendMessage(msg)
            ma?.accept(ac)
            ac.queue(m)
        }
    }

    fun reply(
        msg: MessageEmbed?,
        ma: Consumer<MessageCreateAction>? = null,
        ra: Consumer<ReplyCallbackAction>? = null,
        m: Consumer<Message>? = null,
        ih: Consumer<InteractionHook>? = null
    ) {
        if (slashCommandEvent != null) {
            val reply = slashCommandEvent.replyEmbeds(msg!!)
            ra?.accept(reply)
            reply.queue(ih)
        } else {
            val ac = channel.sendMessageEmbeds(msg!!)
            ma?.accept(ac)
            ac.queue(m)
        }
        logger.info("QUEUED! " + System.currentTimeMillis())
    }

    fun reply(message: MessageEmbed?, ephemeral: Boolean = false) {
        if (slashCommandEvent != null) slashCommandEvent.replyEmbeds(message!!).setEphemeral(ephemeral)
            .queue() else channel.sendMessageEmbeds(
            message!!
        ).queue()
    }

    fun done(ephemeral: Boolean = false) {
        reply("Done!", ephemeral)
    }

    val isNotFlo: Boolean
        get() = author.idLong != Constants.FLOID

    fun deferReply(ephermal: Boolean = false) {
        slashCommandEvent?.deferReply()?.setEphemeral(ephermal)?.queue()
    }

    @Suppress("FunctionName")
    fun reply_(
        content: String = SendDefaults.content,
        embeds: Collection<MessageEmbed> = SendDefaults.embeds,
        components: Collection<LayoutComponent> = SendDefaults.components,
        files: Collection<FileUpload> = emptyList(),
        tts: Boolean = false,
        mentions: Mentions = Mentions.default(),
        ephemeral: Boolean = SendDefaults.ephemeral
    ) {
        slashCommandEvent!!.reply_(content, embeds, components, files, tts, mentions, ephemeral).queue()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GenericCommandEvent::class.java)
    }
}
