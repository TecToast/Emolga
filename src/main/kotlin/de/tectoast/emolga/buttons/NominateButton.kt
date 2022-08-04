package de.tectoast.emolga.buttons

import de.tectoast.emolga.commands.Command
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle

class NominateButton : ButtonListener("nominate") {
    override suspend fun process(e: ButtonInteractionEvent, name: String) {
        val n = Command.nominateButtons[e.messageIdLong]
        if (n == null) {
            e.reply("Diese Nachricht ist veraltet! Nutze erneut `!nominate`!").queue()
            return
        }
        when (e.component.style) {
            ButtonStyle.PRIMARY -> {
                n.unnominate(name)
                n.render(e)
            }
            ButtonStyle.SECONDARY -> {
                n.nominate(name)
                n.render(e)
            }
            ButtonStyle.SUCCESS -> {
                n.finish(e, name == "FINISHNOW")
            }
            ButtonStyle.DANGER -> {
                n.render(e)
            }
            else -> {}
        }
    }
}