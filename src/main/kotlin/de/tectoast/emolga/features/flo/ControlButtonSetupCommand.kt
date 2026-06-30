package de.tectoast.emolga.features.flo

import de.tectoast.emolga.discord.ChannelInterface
import de.tectoast.emolga.domain.guildspecific.remoteservercontrol.model.RemoteServerControlFeature
import de.tectoast.emolga.domain.guildspecific.remoteservercontrol.repository.RemoteServerControlRepository
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.features.various.ControlCentralButton
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.k18n
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.into
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.emoji.Emoji
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class ControlButtonSetupCommand(
    private val remoteServerControlRepo: RemoteServerControlRepository,
    private val remoteServerControlButton: RemoteServerControlButton,
    private val controlCentralButton: ControlCentralButton,
    private val channelInterface: ChannelInterface
) :
    CommandFeature<ControlButtonSetupCommand.Args>(::Args, CommandSpec("controlbuttonsetup", "lol".k18n)) {
    class Args : Arguments() {
        var type by enumBasic<ControlButtonType>("type", "type".k18n)
    }

    enum class ControlButtonType {
        CONTROLCENTRAL, REMOTE_SERVER_CONTROL;
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.done(true)
        when (e.type) {
            CONTROLCENTRAL -> {
                val embed = Embed(title = "Kontrollzentrale", color = Constants.EMBED_COLOR).into()
                val components = listOf(
                    controlCentralButton("Breakpoint".k18n, ButtonStyle.SUCCESS) {
                        mode = ControlCentralButton.Mode.BREAKPOINT
                    },
                    controlCentralButton("Log-Config reloaden".k18n, ButtonStyle.PRIMARY) {
                        mode = ControlCentralButton.Mode.RELOAD_LOG_CONFIG
                    },
                ).into()
                channelInterface.sendMessage(iData.tc, MessageCreate(embeds = embed, components = components))
            }

            REMOTE_SERVER_CONTROL -> {
                for (control in remoteServerControlRepo.getAll()) {
                    val embed = Embed(title = "${control.name} Control", color = Constants.EMBED_COLOR).into()
                    val components = buildList {
                        val features = control.config.features
                        if (RemoteServerControlFeature.START in features) add(
                            remoteServerControlButton(
                                "Server starten".k18n,
                                ButtonStyle.SUCCESS,
                                emoji = Emoji.fromUnicode("⬆\uFE0F")
                            ) { this.pc = control.name; this.action = RemoteServerControlButton.Action.START })

                        if (RemoteServerControlFeature.STATUS in features) add(
                            remoteServerControlButton(
                                "Status".k18n, ButtonStyle.PRIMARY, emoji = Emoji.fromUnicode("ℹ")
                            ) { this.pc = control.name; this.action = RemoteServerControlButton.Action.STATUS })

                        if (RemoteServerControlFeature.STOP in features) add(
                            remoteServerControlButton(
                                "Server stoppen".k18n,
                                ButtonStyle.SECONDARY,
                                emoji = Emoji.fromUnicode("⬇\uFE0F")
                            ) { this.pc = control.name; this.action = RemoteServerControlButton.Action.STOP })

                        if (RemoteServerControlFeature.POWEROFF in features) add(
                            remoteServerControlButton(
                                "PowerOff".k18n, ButtonStyle.DANGER, emoji = Emoji.fromUnicode("⚠")
                            ) { this.pc = control.name; this.action = RemoteServerControlButton.Action.POWEROFF })
                    }.into()
                    channelInterface.sendMessage(iData.tc, MessageCreate(embeds = embed, components = components))
                }
            }
        }
    }
}
