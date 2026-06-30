package de.tectoast.emolga.domain.game.model

sealed interface ResultMessage {
    data class Game(val description: String) : ResultMessage
    data class IllusionWarning(val playerName: String) : ResultMessage
    data class KillsDeathsNotMatching(val illusion: Boolean) : ResultMessage
}