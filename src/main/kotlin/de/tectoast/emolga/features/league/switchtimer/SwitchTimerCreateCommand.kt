package de.tectoast.emolga.features.league.switchtimer

import de.tectoast.emolga.domain.league.draft.service.timer.SwitchTimerService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.league.K18n_SwitchTimer
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.onFailureReply
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class SwitchTimerCreateCommand(private val service: SwitchTimerService, private val btn: SwitchTimerButton) :
    CommandFeature<SwitchTimerCreateCommand.Args>(
        ::Args, CommandSpec(
            "switchtimercreate",
            K18n_SwitchTimer.Help,
        )
    ) {
    class Args : Arguments() {
        var league by string("Liga", K18n_SwitchTimer.ArgLeague)
        var settings by list("Timer %s", K18n_SwitchTimer.ArgSettings, 5, 1)
        var stallSeconds by int("Stall-Sekunden", K18n_SwitchTimer.ArgStallSeconds) {
            default = 0
        }
        var from by int("Startstunde", K18n_SwitchTimer.ArgFrom) {
            default = 0
        }
        var to by int("Endstunde", K18n_SwitchTimer.ArgTo) {
            default = 24
        }
    }

    init {
        restrict(admin)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        val switchTimer =
            service.create(e.league, e.settings, e.stallSeconds, e.from, e.to).onFailureReply() ?: return
        val controlPanel = switchTimer.createControlPanel(e.league, btn)
        iData.replyRaw(ephemeral = false, embeds = controlPanel.first, components = controlPanel.second)
    }
}