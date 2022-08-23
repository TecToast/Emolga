package de.tectoast.emolga.buttons

import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.GPIOManager
import de.tectoast.emolga.utils.GPIOManager.PC
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import java.awt.Color
import java.io.IOException

class FlorixButton : ButtonListener("florix") {
    @Throws(IOException::class)
    override suspend fun process(e: ButtonInteractionEvent, name: String) {
        if (e.user.idLong != Constants.FLOID || e.guild!!.idLong != Constants.G.MY) return
        val mid = e.messageIdLong
        val split = name.split(":")
        val pc = PC.byMessage(if (name.contains(":")) split[1].toLong() else mid)
        val on = GPIOManager.isOn(pc)
        when (split[0]) {
            "startserver" -> {
                if (on) {
                    e.reply_("Der Server ist bereits an!", ephemeral = true).queue()
                    return
                }
                GPIOManager.startServer(pc)
                e.reply_("Der Server wurde gestartet!", ephemeral = true).queue()
            }
            "stopserver" -> {
                if (!on) {
                    e.reply_("Der Server ist bereits aus!", ephemeral = true).queue()
                    return
                }
                e.reply_(
                    embed = Embed(
                        title = "Bist du dir sicher, dass du den Server herunterfahren möchtest?",
                        color = Color.RED.rgb
                    ), components = listOf(
                        Button.danger(
                            "florix;stopserverreal:$mid", "Ja"
                        ), Button.success("florix;no", "Nein")
                    ).into()
                )
                    .queue()
            }
            "poweroff" -> {
                if (!on) {
                    e.reply_("Der Server ist bereits aus!", ephemeral = true).queue()
                    return
                }
                e.reply_(
                    embed = Embed(
                        title = "Bist du dir sicher, dass POWEROFF aktiviert werden soll?",
                        color = Color.RED.rgb
                    ), components = listOf(
                        Button.danger(
                            "florix;poweroffreal:$mid", "Ja"
                        ), Button.success("florix;no", "Nein")
                    ).into()
                )
                    .queue()
            }
            "status" -> e.reply_("Der Server ist ${if (on) "an" else "aus"}!", ephemeral = true).queue()
            "stopserverreal" -> {
                if (!on) {
                    e.reply_("Der Server ist bereits aus!", ephemeral = true).queue()
                    return
                }
                GPIOManager.stopServer(pc)
                e.reply_("Der Server wurde heruntergefahren!", ephemeral = true)
                    .queue { it.deleteMessageById(e.messageId).queue() }
            }
            "poweroffreal" -> {
                if (!on) {
                    e.reply_("Der Server ist bereits aus!", ephemeral = true).queue()
                    return
                }
                GPIOManager.powerOff(pc)
                e.reply_("Power-Off wurde aktiviert!", ephemeral = true)
                    .queue { it.deleteMessageById(e.messageId).queue() }
            }
        }
    }
}