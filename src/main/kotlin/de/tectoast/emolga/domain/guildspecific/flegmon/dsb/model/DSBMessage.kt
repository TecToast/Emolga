package de.tectoast.emolga.domain.guildspecific.flegmon.dsb.model

import de.tectoast.emolga.domain.eventbus.EmolgaEvent
import kotlinx.serialization.Serializable


@Serializable
data class DSBMessage(val userId: String, val text: String, val url: String? = null, val timestamp: String) :
    EmolgaEvent
