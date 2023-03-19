package de.tectoast.emolga.modals

import de.tectoast.emolga.commands.httpClient
import de.tectoast.emolga.database.exposed.Crinchy
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class CrinchyModal : ModalListener("crinchy") {

    companion object {

    }

    override suspend fun process(e: ModalInteractionEvent, name: String?) {
        e.deferReply().queue()
        e.getValue("replays")!!.asString.split("\n").asFlow()
            .map { delay(2000); it to httpClient.get("$it.log").bodyAsText().split("\n"); }
            .collect {
                Crinchy.insertReplay(it.first.substringAfterLast("/"), it.second)
            }
        e.hook.sendMessage("Neue Statistik:\n${Crinchy.allStats()}").queue()
    }

}
