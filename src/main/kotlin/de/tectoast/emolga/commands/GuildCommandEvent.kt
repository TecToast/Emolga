package de.tectoast.emolga.commands

import de.tectoast.emolga.commands.Command.ArgumentManager
import de.tectoast.emolga.utils.Constants
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class GuildCommandEvent : GenericCommandEvent {
    val member: Member
    val guild: Guild
    val textChannel: GuildMessageChannel
    val usedName: String
    val command: Command
    lateinit var arguments: ArgumentManager
        private set


    constructor(c: Command, e: MessageReceivedEvent) : super(e.message) {
        member = e.member!!
        guild = e.guild
        this.textChannel = e.guildChannel
        command = c
        usedName = Command.WHITESPACES_SPLITTER.split(msg!!)[0].substring(c.prefix.length)
    }

    suspend fun initialize(c: Command, e: MessageReceivedEvent) {
        arguments = c.argumentTemplate.construct(e, c)
    }

    suspend fun execute() {
        coroutineScope {
            this.launch {
                try {
                    arguments.executor.process(this@GuildCommandEvent)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    send(
                        "Es ist ein Fehler beim Ausführen des Commands aufgetreten!\nWenn du denkst, dass dies ein interner Fehler beim Bot ist, melde dich bitte bei Flo (${Constants.MYTAG})."
                            .condAppend(
                                member.idLong == Constants.FLOID,
                                "\nJa Flo, du sollst dich auch bei ihm melden du Kek :^)"
                            )
                    )
                    Command.sendToMe("Error " + textChannel.asMention + " Server: " + textChannel.guild.name + " Msg: " + msg + " View Console")
                }
            }
        }
    }

    private fun send(msg: String) {
        slashCommandEvent?.reply(msg)?.queue() ?: textChannel.sendMessage(msg).queue()
    }

    constructor(c: Command, e: SlashCommandInteractionEvent) : super(e) {
        member = e.member!!
        guild = e.guild!!
        command = c
        this.textChannel = e.guildChannel
        usedName = e.name
    }

    suspend fun initialize(c: Command, e: SlashCommandInteractionEvent) {
        arguments = c.argumentTemplate.construct(e, c)
    }

}
