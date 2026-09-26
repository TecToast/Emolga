package de.tectoast.emolga.domain.league.draft.model.core

import de.tectoast.emolga.domain.eventbus.EmolgaEvent

data class PicksModifiedEvent(val guild: Long) : EmolgaEvent