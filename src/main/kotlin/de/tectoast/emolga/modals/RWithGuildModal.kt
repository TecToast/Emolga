package de.tectoast.emolga.modals

import de.tectoast.emolga.commands.Command.Companion.analyseReplay
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

object RWithGuildModal : ModalListener("rwithguild") {
    override suspend fun process(e: ModalInteractionEvent, name: String?) {
        val id = e.getValue("id")!!.asString
        val urls = e.getValue("urls")!!.asString
        urls.split("\n").forEach {
            analyseReplay(it, resultchannelParam = e.channel.asTextChannel(), customGuild = id.toLong())
        }
        e.reply("Replays wurden analysiert!").setEphemeral(true).queue()
    }
}
