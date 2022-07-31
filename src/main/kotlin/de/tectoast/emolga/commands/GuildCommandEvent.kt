package de.tectoast.emolga.commands

import de.tectoast.emolga.commands.Command.ArgumentException
import de.tectoast.emolga.commands.Command.ArgumentManager
import de.tectoast.emolga.utils.Constants.FLOID
import de.tectoast.emolga.utils.Constants.MYTAG
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class GuildCommandEvent : GenericCommandEvent {
    val member: Member
    val guild: Guild
    val textChannel: TextChannel
    val usedName: String
    var arguments: ArgumentManager

    @kotlin.jvm.Throws(ArgumentException::class)
    constructor(c: Command, e: MessageReceivedEvent) : super(e.message) {
        member = e.member!!
        guild = e.guild
        this.textChannel = e.guildChannel.asTextChannel()
        val template = c.argumentTemplate
        arguments = template.construct(e, c)
        usedName = Command.WHITESPACES_SPLITTER.split(msg!!)[0].substring(c.prefix.length)
        Thread({
            try {
                arguments.executor.process(this)
            } catch (ex: Exception) {
                ex.printStackTrace()
                textChannel.sendMessage(
                    "Es ist ein Fehler beim Ausführen des Commands aufgetreten!\nWenn du denkst, dass dies ein interner Fehler beim Bot ist, melde dich bitte bei Flo ($MYTAG).\n${
                        c.getHelp(
                            e.guild
                        )
                    }${if (member.idLong == FLOID) "\nJa Flo, du sollst dich auch bei ihm melden du Kek! :^)" else ""}"
                ).queue()
            }
        }, "CMD " + c.name).start()
    }

    constructor(c: Command, e: SlashCommandInteractionEvent) : super(e) {
        member = e.member!!
        guild = e.guild!!
        this.textChannel = e.guildChannel.asTextChannel()
        usedName = e.name
        val template = c.argumentTemplate
        arguments = template.construct(e, c)
        Thread({
            try {
                arguments.executor.process(this)
            } catch (ex: Exception) {
                ex.printStackTrace()
                textChannel.sendMessage(
                    "Es ist ein Fehler beim Ausführen des Commands aufgetreten!\nWenn du denkst, dass dies ein interner Fehler beim Bot ist, melde dich bitte bei Flo ($MYTAG).\n${
                        c.getHelp(
                            guild
                        )
                    }${if (member.idLong == FLOID) "\nJa Flo, du sollst dich auch bei ihm melden du Kek! :^)" else ""}"
                ).queue()
            }
        }, "CMD " + c.name).start()
    }

    fun deleteMessage() {
        message?.delete()?.queue()
    }
}