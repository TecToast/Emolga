package de.tectoast.emolga.domain.league.draft.model.timer

import kotlinx.serialization.Serializable

@Serializable
data class TimerData(val from: Int, val to: Int)
