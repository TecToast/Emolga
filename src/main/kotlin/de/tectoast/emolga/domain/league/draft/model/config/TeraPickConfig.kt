package de.tectoast.emolga.domain.league.draft.model.config

import kotlinx.serialization.Serializable

@Serializable
data class TeraPickConfig(val tlIdentifier: String = "TERA", val amount: Int = 1, val maxPoints: Int? = null)
