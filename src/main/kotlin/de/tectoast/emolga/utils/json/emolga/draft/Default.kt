package de.tectoast.emolga.utils.json.emolga.draft

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Default")
class Default : League() {
    override val teamsize = 69
    override val duringTimerSkipMode: DuringTimerSkipMode?
        get() = defaultDuringTimerSkipMode

    override val afterTimerSkipMode: AfterTimerSkipMode
        get() = defaultAfterTimerSkipMode
}

var defaultDuringTimerSkipMode: DuringTimerSkipMode? = null
var defaultAfterTimerSkipMode: AfterTimerSkipMode = AFTER_DRAFT_UNORDERED
