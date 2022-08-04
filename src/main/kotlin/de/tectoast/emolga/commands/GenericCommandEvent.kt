package de.tectoast.emolga.commands

import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.records.DeferredSlashResponse
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.requests.restaction.MessageAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
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
    var slashCommandAcknowlegded = false
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
        mentionedChannels =
            e.options.filter { it.type == OptionType.CHANNEL }.map { it.asChannel.asTextChannel() }
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

    @JvmOverloads
    fun reply(msg: String, ephermal: Boolean = false) {
        if (msg.isEmpty()) return
        if (slashCommandEvent != null) slashCommandEvent.reply(msg).setEphemeral(ephermal)
            .queue().also { slashCommandAcknowlegded = true } else channel.sendMessage(msg).queue()
    }

    fun reply(
        msg: String,
        ma: Consumer<MessageAction>? = null,
        ra: Consumer<ReplyCallbackAction>? = null,
        m: Consumer<Message>? = null,
        ih: Consumer<InteractionHook>? = null
    ) {
        if (slashCommandEvent != null) {
            val reply = slashCommandEvent.reply(msg)
            ra?.accept(reply)
            reply.queue(ih).also { slashCommandAcknowlegded = true }
        } else {
            val ac = channel.sendMessage(msg)
            ma?.accept(ac)
            ac.queue(m)
        }
    }

    fun reply(
        msg: MessageEmbed?,
        ma: Consumer<MessageAction>? = null,
        ra: Consumer<ReplyCallbackAction>? = null,
        m: Consumer<Message>? = null,
        ih: Consumer<InteractionHook>? = null
    ) {
        if (slashCommandEvent != null) {
            val reply = slashCommandEvent.replyEmbeds(msg!!)
            ra?.accept(reply)
            reply.queue(ih).also { slashCommandAcknowlegded = true }
        } else {
            val ac = channel.sendMessageEmbeds(msg!!)
            ma?.accept(ac)
            ac.queue(m)
        }
        logger.info("QUEUED! " + System.currentTimeMillis())
    }

    fun reply(message: MessageEmbed?) {
        if (slashCommandEvent != null) slashCommandEvent.replyEmbeds(message!!).queue()
            .also { slashCommandAcknowlegded = true } else channel.sendMessageEmbeds(
            message!!
        ).queue()
    }

    fun done() {
        reply("Done!")
    }

    val isNotFlo: Boolean
        get() = author.idLong != Constants.FLOID

    fun deferReply(): DeferredSlashResponse? {
        return if (slashCommandEvent != null) DeferredSlashResponse(
            slashCommandEvent.deferReply().submit()
        ).also { slashCommandAcknowlegded = true } else null
    }

    val isSlash: Boolean
        get() = slashCommandEvent != null

    companion object {
        private val logger = LoggerFactory.getLogger(GenericCommandEvent::class.java)
    }
}