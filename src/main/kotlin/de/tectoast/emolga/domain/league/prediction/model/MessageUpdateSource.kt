package de.tectoast.emolga.domain.league.prediction.model

import de.tectoast.emolga.features.interaction.InteractionData

sealed interface MessageUpdateSource {
    data class MessageId(val id: Long) : MessageUpdateSource
    data class WithInteractionData(val iData: InteractionData) : MessageUpdateSource
}