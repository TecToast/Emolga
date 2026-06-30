package de.tectoast.emolga.domain.league.draft.model.core

data class ValidationSuccess(val saveTier: String, val freePick: Boolean, val updrafted: Boolean, val points: Int?)
