package de.tectoast.emolga.modals

import de.tectoast.emolga.database.exposed.CrinchyDB
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class CrinchyModal : ModalListener("crinchy") {

    override suspend fun process(e: ModalInteractionEvent, name: String?) {
        e.deferReply().queue()
        CrinchyDB.insertReplays(e.getValue("replays")!!.asString.split("\n"))
        e.hook.sendMessage("Neue Statistik:\n${CrinchyDB.allStats()}").queue()
    }

}
