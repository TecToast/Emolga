package de.tectoast.emolga.features.league.switchtimer

import de.tectoast.emolga.domain.league.draft.service.timer.SwitchTimerService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.league.K18n_SwitchTimer
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.ButtonSpec
import de.tectoast.emolga.features.system.types.ButtonFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.onFailureReply
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class SwitchTimerButton(private val service: SwitchTimerService) : ButtonFeature<SwitchTimerButton.Args>(
    ::Args,
    ButtonSpec("switchtimer")
) {
    override val buttonStyle = ButtonStyle.PRIMARY

    class Args : Arguments() {
        var league by string()
        var switchTo by string()
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        val switchTimer = service.switchTo(e.league, e.switchTo).onFailureReply() ?: return
        iData.deferEdit()
        val controlPanel = switchTimer.createControlPanel(e.league, this)
        iData.edit(contentK18n = null, embeds = controlPanel.first, components = controlPanel.second)
        iData.reply(K18n_SwitchTimer.Success(e.switchTo), ephemeral = true)
    }
}