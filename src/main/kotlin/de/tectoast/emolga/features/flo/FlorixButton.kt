package de.tectoast.emolga.features.flo

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.ButtonFeature
import de.tectoast.emolga.features.ButtonSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.flo.FlorixButton.Action.*
import de.tectoast.emolga.utils.GPIOManager
import de.tectoast.emolga.utils.PC
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.into
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.DANGER
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.SUCCESS
import java.awt.Color

object FlorixButton : ButtonFeature<FlorixButton.Args>(::Args, ButtonSpec("florix")) {
    class Args : Arguments() {
        var pc by enumBasic<PC>()
        var action by enumBasic<Action>()
    }

    enum class Action {
        START, STOP, POWEROFF, STATUS, STOPREAL, POWEROFFREAL, NO
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        val gpio = GPIOManager()
        val pc = e.pc
        val on = gpio.isOn(pc)
        iData.ephemeralDefault()
        fun components(act: Action) =
            listOf(FlorixButton("Ja", DANGER) { this.pc = pc; action = act },
                FlorixButton("Nein", SUCCESS) { this.pc = pc; action = NO }).into()

        when (e.action) {
            START -> {
                if (on) return iData.reply("Der Server ist bereits an!")
                gpio.startServer(pc)
                iData.reply("Der Server wurde gestartet!")
            }

            STOP -> {
                if (!on) return iData.reply("Der Server ist bereits aus!")
                iData.reply(
                    embeds = Embed(
                        title = "Bist du dir sicher, dass du den Server herunterfahren möchtest?", color = Color.RED.rgb
                    ).into(), components = components(STOPREAL)
                )
            }

            POWEROFF -> {
                if (!on) return iData.reply("Der Server ist bereits aus!")
                iData.reply(
                    embeds = Embed(
                        title = "Bist du dir sicher, dass POWEROFF aktiviert werden soll?", color = Color.RED.rgb
                    ).into(), components = components(POWEROFFREAL)
                )
            }

            STATUS -> iData.reply("Der Server ist ${if (on) "an" else "aus"}!")
            STOPREAL -> {
                if (!on) return iData.reply("Der Server ist bereits aus!")
                gpio.stopServer(pc)
                iData.replyAwait("Der Server wurde heruntergefahren!")
                iData.textChannel.deleteMessageById(iData.message.id).queue()
            }

            POWEROFFREAL -> {
                if (!on) return iData.reply("Der Server ist bereits aus!")
                gpio.powerOff(pc)
                iData.replyAwait("POWEROFF wurde aktiviert!")
                iData.textChannel.deleteMessageById(iData.message.id).queue()
            }

            NO -> {
                iData.reply("Aktion abgebrochen!")
                iData.textChannel.deleteMessageById(iData.message.id).queue()
            }
        }
    }
}
