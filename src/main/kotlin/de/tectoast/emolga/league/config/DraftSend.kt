package de.tectoast.emolga.league.config

import kotlinx.serialization.Serializable

@Serializable
data class DraftSendConfig(
    val alwaysSendTier: Boolean = false
)