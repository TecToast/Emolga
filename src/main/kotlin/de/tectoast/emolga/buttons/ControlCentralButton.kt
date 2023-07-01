package de.tectoast.emolga.buttons

import de.tectoast.emolga.commands.PrivateCommands
import de.tectoast.emolga.utils.draft.Tierlist
import dev.minn.jda.ktx.messages.send
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class ControlCentralButton : ButtonListener("controlcentral") {
    override suspend fun process(e: ButtonInteractionEvent, name: String) {
        var b = true
        var breakpoint = false
        e.deferReply(true).queue()
        when (name) {
            "updateslash" -> PrivateCommands.updateSlashCommands()
            "updatetierlist" -> Tierlist.setup()
            "breakpoint" -> breakpoint = true
            else -> b = false
        }
        e.hook.send(if (b) "Done!" else "Not recognized! $name", ephemeral = true).queue()
        if (breakpoint) {
            print("") // I have a JVM breakpoint here
        }
    }
}
