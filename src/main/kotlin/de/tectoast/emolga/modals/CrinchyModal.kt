package de.tectoast.emolga.modals

import de.tectoast.emolga.database.exposed.Crinchy
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class CrinchyModal : ModalListener("crinchy") {

    companion object {

    }

    override suspend fun process(e: ModalInteractionEvent, name: String?) {
        e.deferReply().queue()
        Crinchy.insertReplays(e.getValue("replays")!!.asString.split("\n"))
        e.hook.sendMessage("Neue Statistik:\n${Crinchy.allStats()}").queue()
    }

}
