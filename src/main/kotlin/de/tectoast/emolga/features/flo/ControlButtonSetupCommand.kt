package de.tectoast.emolga.features.flo

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.various.ControlCentralButton
import de.tectoast.emolga.utils.embedColor
import de.tectoast.emolga.utils.json.RemoteServerControlFeature
import de.tectoast.emolga.utils.json.db
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.emoji.Emoji

object ControlButtonSetupCommand :
    CommandFeature<ControlButtonSetupCommand.Args>(::Args, CommandSpec("controlbuttonsetup", "lol")) {
    class Args : Arguments() {
        var type by enumBasic<ControlButtonType>("type", "type")
    }

    enum class ControlButtonType {
        CONTROLCENTRAL, FLORIX;
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.done(true)
        when (e.type) {
            ControlButtonType.CONTROLCENTRAL -> {
                val embed = Embed(title = "Kontrollzentrale", color = embedColor).into()
                val components = listOf(
                    ControlCentralButton("Tierlist updaten", ButtonStyle.PRIMARY) {
                        mode = ControlCentralButton.Mode.UPDATE_TIERLIST
                    },
                    ControlCentralButton("Breakpoint", ButtonStyle.SUCCESS) {
                        mode = ControlCentralButton.Mode.BREAKPOINT
                    },
                    ControlCentralButton("Log-Config reloaden", ButtonStyle.PRIMARY) {
                        mode = ControlCentralButton.Mode.RELOAD_LOG_CONFIG
                    },
                ).into()
                iData.textChannel.send(embeds = embed, components = components).queue()
            }

            ControlButtonType.FLORIX -> {
                for (control in db.remoteServerControl.find().toList()) {
                    val embed = Embed(title = "${control.name} Control", color = embedColor).into()
                    val components = buildList {
                        val features = control.features
                        if (RemoteServerControlFeature.START in features) add(
                            FlorixButton(
                                "Server starten",
                                ButtonStyle.SUCCESS,
                                emoji = Emoji.fromCustom("stonks", 964570148692443196, false)
                            ) { this.pc = control.name; this.action = FlorixButton.Action.START })

                        if (RemoteServerControlFeature.STATUS in features) add(
                            FlorixButton(
                                "Status", ButtonStyle.PRIMARY, emoji = Emoji.fromUnicode("ℹ")
                            ) { this.pc = control.name; this.action = FlorixButton.Action.STATUS })

                        if (RemoteServerControlFeature.STOP in features) add(
                            FlorixButton(
                                "Server stoppen",
                                ButtonStyle.SECONDARY,
                                emoji = Emoji.fromCustom("notstonks", 964570147220254810, false)
                            ) { this.pc = control.name; this.action = FlorixButton.Action.STOP })

                        if (RemoteServerControlFeature.POWEROFF in features) add(
                            FlorixButton(
                                "PowerOff", ButtonStyle.DANGER, emoji = Emoji.fromUnicode("⚠")
                            ) { this.pc = control.name; this.action = FlorixButton.Action.POWEROFF })
                    }.into()
                    iData.textChannel.send(embeds = embed, components = components).queue()
                }
            }
        }
    }
}