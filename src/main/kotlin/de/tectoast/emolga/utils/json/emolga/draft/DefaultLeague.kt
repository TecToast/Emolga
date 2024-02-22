package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.utils.DraftTimer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Default")
class DefaultLeague : League() {
    override val teamsize = 69
    override val duringTimerSkipMode by DefaultLeagueSettings::duringTimerSkipMode

    override val afterTimerSkipMode by DefaultLeagueSettings::afterTimerSkipMode


    override var timer: DraftTimer? by DefaultLeagueSettings::timer
}

object DefaultLeagueSettings {
    var timer: DraftTimer? = null
    var duringTimerSkipMode: DuringTimerSkipMode? = null
    var afterTimerSkipMode: AfterTimerSkipMode = AFTER_DRAFT_UNORDERED

    operator fun invoke(func: DefaultLeagueSettings.() -> Unit) = func(this)

    fun reset() {
        timer = null
        duringTimerSkipMode = null
        afterTimerSkipMode = AFTER_DRAFT_UNORDERED
    }
}
