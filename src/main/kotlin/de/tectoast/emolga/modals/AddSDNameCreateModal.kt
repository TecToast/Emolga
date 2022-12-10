package de.tectoast.emolga.modals

import de.tectoast.emolga.commands.notFromFlo
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.primary
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class AddSDNameCreateModal : ModalListener("addsdnamecreate") {
    override suspend fun process(e: ModalInteractionEvent, name: String?) {
        if (e.notFromFlo) return
        val (tc, msg, buttonname) = listOf("tc", "msg", "buttonname").map { e.getValue(it)!!.asString }
        runCatching {
            e.jda.getTextChannelById(tc)!!
                .send(content = msg, components = primary(id = "addsdname", label = buttonname).into()).await()
        }.fold(
            onSuccess = { e.reply("Erfolgreich erstellt!").setEphemeral(true).queue() },
            onFailure = { e.reply("Fehler beim erstellen!").setEphemeral(true).queue() }
        )
    }
}
