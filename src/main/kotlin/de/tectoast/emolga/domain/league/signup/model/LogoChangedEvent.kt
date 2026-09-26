package de.tectoast.emolga.domain.league.signup.model

import de.tectoast.emolga.domain.eventbus.EmolgaEvent

data class LogoChangedEvent(val guild: Long, val userId: Long) : EmolgaEvent