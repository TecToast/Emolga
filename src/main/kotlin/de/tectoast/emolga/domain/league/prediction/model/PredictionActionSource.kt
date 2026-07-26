package de.tectoast.emolga.domain.league.prediction.model

import de.tectoast.emolga.features.interaction.InteractionData

sealed interface PredictionActionSource {
    data object Direct : PredictionActionSource
    data class WithInteractionData(val iData: InteractionData) : PredictionActionSource
}