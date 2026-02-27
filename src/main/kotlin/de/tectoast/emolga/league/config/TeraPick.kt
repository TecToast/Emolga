package de.tectoast.emolga.league.config

import kotlinx.serialization.Serializable

@Serializable
data class TeraPickConfig(val tlIdentifier: String? = "TERA", val amount: Int = 1)