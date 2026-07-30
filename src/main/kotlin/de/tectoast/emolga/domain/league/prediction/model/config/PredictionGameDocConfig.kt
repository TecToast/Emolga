package de.tectoast.emolga.domain.league.prediction.model.config

import kotlinx.serialization.Serializable

@Serializable
data class PredictionGameDocConfig(val sheetId: String, val sheet: String, val x: Int, val y: Int)
