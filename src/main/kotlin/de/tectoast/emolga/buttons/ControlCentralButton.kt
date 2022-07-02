package de.tectoast.emolga.buttons

import de.tectoast.emolga.commands.Command
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class ControlCentralButton : ButtonListener("controlcentral") {
    override fun process(e: ButtonInteractionEvent, name: String) {
        var b = true
        when (name) {
            "ej" -> Command.emolgaJSON = Command.load("./emolgadata.json")
            "saveemolgajson" -> Command.saveEmolgaJSON()
            else -> b = false
        }
        if (b) e.reply("Done!").setEphemeral(true).queue() else e.reply("Not recognized! $name").setEphemeral(true)
            .queue()
    }
}