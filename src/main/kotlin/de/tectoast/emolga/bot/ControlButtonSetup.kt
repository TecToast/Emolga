package de.tectoast.emolga.bot

import de.tectoast.emolga.features.flo.FlorixButton
import de.tectoast.emolga.features.various.ControlCentralButton
import de.tectoast.emolga.utils.PC
import de.tectoast.emolga.utils.embedColor
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.editMessage
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle

object ControlButtonSetup {
    private val CONTROLCENTRALGENERATION = ControlCentralData(true, 967890099029278740, 967890640065134602)
    private val FLORIXCONTROLGENERATION = FlorixControlData(
        false, 964528154549055558, mapOf(
            PC.FLORIX_2 to 964571226964115496, PC.FLORIX_4 to 975076826588282962
        )
    )

    /**
     * Initializes the control buttons in the control central and florix channels
     */
    fun init() {
        CONTROLCENTRALGENERATION.takeIf { it.enabled }?.let {
            val tc = jda.getTextChannelById(it.tc)!!
            val embed = Embed(title = "Kontrollzentrale", color = embedColor).into()
            val components = listOf(
                ControlCentralButton("Tierlist updaten", ButtonStyle.PRIMARY) {
                    mode = ControlCentralButton.Mode.UPDATE_TIERLIST
                },
                ControlCentralButton("Breakpoint", ButtonStyle.SUCCESS) { mode = ControlCentralButton.Mode.BREAKPOINT },
                ControlCentralButton("Log-Config reloaden", ButtonStyle.PRIMARY) {
                    mode = ControlCentralButton.Mode.RELOAD_LOG_CONFIG
                },
            ).into()
            it.mid?.let { mid ->
                tc.editMessage(
                    mid.toString(), embeds = embed, components = components
                ).queue()
            } ?: tc.send(embeds = embed, components = components).queue()
        }
        FLORIXCONTROLGENERATION.takeIf { it.enabled }?.let {
            val tc = jda.getTextChannelById(it.tc)!!
            PC.entries.forEach { pc ->
                val mid = it.mids[pc]
                val id = pc.name.takeLast(1)
                val embed = Embed(title = "Florix$id Control", color = embedColor).into()
                val components = listOf(
                    FlorixButton(
                        "Server starten",
                        ButtonStyle.SUCCESS,
                        emoji = Emoji.fromCustom("stonks", 964570148692443196, false)
                    ) { this.pc = pc; this.action = FlorixButton.Action.START },
                    FlorixButton(
                        "Server stoppen",
                        ButtonStyle.SECONDARY,
                        emoji = Emoji.fromCustom("notstonks", 964570147220254810, false)
                    ) { this.pc = pc; this.action = FlorixButton.Action.STOP },
                    FlorixButton(
                        "PowerOff", ButtonStyle.DANGER, emoji = Emoji.fromUnicode("⚠")
                    ) { this.pc = pc; this.action = FlorixButton.Action.POWEROFF },
                    FlorixButton(
                        "Status", ButtonStyle.PRIMARY, emoji = Emoji.fromUnicode("ℹ")
                    ) { this.pc = pc; this.action = FlorixButton.Action.STATUS },
                ).into()
                mid?.let { _ ->
                    tc.editMessage(
                        mid.toString(), embeds = embed, components = components
                    ).queue()
                } ?: tc.send(embeds = embed, components = components).queue()
            }
        }
    }

    private data class ControlCentralData(val enabled: Boolean, val tc: Long, val mid: Long?)
    private data class FlorixControlData(val enabled: Boolean, val tc: Long, val mids: Map<PC, Long> = emptyMap())
}
