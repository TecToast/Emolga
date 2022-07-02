package de.tectoast.emolga.buttons

import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.GPIOManager
import de.tectoast.emolga.utils.GPIOManager.PC
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.buttons.Button
import java.awt.Color
import java.io.IOException

class FlorixButton : ButtonListener("florix") {
    @Throws(IOException::class)
    override fun process(e: ButtonInteractionEvent, name: String) {
        if (e.user.idLong != Constants.FLOID || e.guild!!.idLong != Constants.MYSERVER) return
        val mid = e.messageIdLong
        val split = name.split(":".toRegex())
        val pc = PC.byMessage(if (name.contains(":")) split[1].toLong() else mid)
        val on = GPIOManager.isOn(pc)
        when (split[0]) {
            "startserver" -> {
                if (on) {
                    e.reply("Der Server ist bereits an!").setEphemeral(true).queue()
                    return
                }
                GPIOManager.startServer(pc)
                e.reply("Der Server wurde gestartet!").setEphemeral(true).queue()
            }
            "stopserver" -> {
                if (!on) {
                    e.reply("Der Server ist bereits aus!").setEphemeral(true).queue()
                    return
                }
                e.replyEmbeds(
                    EmbedBuilder().setTitle("Bist du dir sicher, dass du den Server herunterfahren möchtest?").setColor(
                        Color.RED
                    ).build()
                ).addActionRow(
                    Button.danger(
                        "florix;stopserverreal:$mid", "Ja"
                    ), Button.success("florix;no", "Nein")
                ).queue()
            }
            "poweroff" -> {
                if (!on) {
                    e.reply("Der Server ist bereits aus!").setEphemeral(true).queue()
                    return
                }
                e.replyEmbeds(
                    EmbedBuilder().setTitle("Bist du dir sicher, dass POWEROFF aktiviert werden soll?").setColor(
                        Color.RED
                    ).build()
                ).addActionRow(
                    Button.danger(
                        "florix;poweroffreal:$mid", "Ja"
                    ), Button.success("florix;no", "Nein")
                ).queue()
            }
            "status" -> e.reply("Der Server ist ${if (on) "an" else "aus"}!").setEphemeral(true).queue()
            "stopserverreal" -> {
                if (!on) {
                    e.reply("Der Server ist bereits aus!").setEphemeral(true).queue()
                    return
                }
                GPIOManager.stopServer(pc)
                e.reply("Der Server wurde heruntergefahren!").setEphemeral(true)
                    .queue { i: InteractionHook -> i.deleteMessageById(e.messageId).queue() }
            }
            "poweroffreal" -> {
                if (!on) {
                    e.reply("Der Server ist bereits aus!").setEphemeral(true).queue()
                    return
                }
                GPIOManager.powerOff(pc)
                e.reply("Power-Off wurde aktiviert!").setEphemeral(true)
                    .queue { i: InteractionHook -> i.deleteMessageById(e.messageId).queue() }
            }
        }
    }
}