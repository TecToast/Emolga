package de.tectoast.emolga.domain.league.prediction.model.config

import kotlinx.serialization.Serializable

@Serializable
enum class PredictionGameCurrentStateType(val rank: Int) {
    ALWAYS(0),
    ON_LOCK(1)
}
