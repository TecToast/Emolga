package de.tectoast.emolga.buttons

import de.tectoast.emolga.commands.Command
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class ControlCentralButton : ButtonListener("controlcentral") {
    override suspend fun process(e: ButtonInteractionEvent, name: String) {
        var b = true
        when (name) {
            "ej" -> /*Command.emolgaJSON = Command.load("./emolgadata.json")*/Command.loadEmolgaJSON()
            "saveemolgajson" -> Command.saveEmolgaJSON()
            else -> b = false
        }
        e.reply_(if (b) "Done!" else "Not recognized! $name", ephemeral = true).queue()
    }
}