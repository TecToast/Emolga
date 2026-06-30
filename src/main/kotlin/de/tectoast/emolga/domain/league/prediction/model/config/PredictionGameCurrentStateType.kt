package de.tectoast.emolga.domain.league.prediction.model.config

import kotlinx.serialization.Serializable

@Serializable
enum class PredictionGameCurrentStateType {
    ALWAYS,
    ON_LOCK
}
