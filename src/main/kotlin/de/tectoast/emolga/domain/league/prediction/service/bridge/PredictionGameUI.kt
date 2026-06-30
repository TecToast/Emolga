package de.tectoast.emolga.domain.league.prediction.service.bridge

import de.tectoast.emolga.domain.league.prediction.model.PredictionMatchViewState

interface PredictionGameUI {
    suspend fun sendInitialMessage(channelId: Long, title: String, color: Int)
    suspend fun sendPredictionGameMessage(state: PredictionMatchViewState): Long
    suspend fun updatePredictionGameMessage(state: PredictionMatchViewState, messageId: Long)
    suspend fun sendRolePing(channelId: Long, roleId: Long)
}