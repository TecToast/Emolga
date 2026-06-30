package de.tectoast.emolga.features.league.switchtimer

import de.tectoast.emolga.domain.league.draft.model.timer.DraftTimerConfig
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.k18n
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.into

fun DraftTimerConfig.SwitchTimer.createControlPanel(leagueName: String, btn: SwitchTimerButton) = Embed {
    title = "Switch-Timer Control-Panel"
    description = "`${currentTimer}`"
    color = Constants.EMBED_COLOR
}.into() to timerInfos.keys.map { name ->
    btn.withoutIData(label = name.k18n) {
        this.league = leagueName
        this.switchTo = name
    }
}.into()