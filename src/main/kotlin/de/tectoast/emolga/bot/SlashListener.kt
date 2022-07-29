package de.tectoast.emolga.bot

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.Command.MissingArgumentException
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants.FLOID
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class SlashListener : ListenerAdapter() {
    override fun onSlashCommandInteraction(e: SlashCommandInteractionEvent) {
        val command = Command.byName(e.name)!!
        val u = e.user
        val tco = e.textChannel
        try {
            GuildCommandEvent(command, e)
        } catch (ex: MissingArgumentException) {
            val arg = ex.argument!!
            e.reply(
                arg.customErrorMessage
                    ?: """Das benötigte Argument `${arg.name}`, was eigentlich ${Command.buildEnumeration(arg.type.getName())} sein müsste, ist nicht vorhanden!
Nähere Informationen über die richtige Syntax für den Command erhältst du unter `e!help ${command.name}`.""".trimIndent()
            )
            if (u.idLong != FLOID) {
                Command.sendToMe("MissingArgument " + tco.asMention + " Server: " + tco.guild.name)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            e.hook.sendMessage(
                """
    Es ist ein Fehler beim Ausführen des Commands aufgetreten!
    Wenn du denkst, dass dies ein interner Fehler beim Bot ist, melde dich bitte bei Flo/TecToast.
    ${command.getHelp(e.guild)}${if (u.idLong == FLOID) "\nJa Flo, du sollst dich auch bei ihm melden du Kek! :^)" else ""}
    """.trimIndent()
            ).queue()
        }
    }
}