package de.tectoast.emolga.domain.game.model.analysis

sealed interface ShowdownLogProvider {
    data class ReplayUrl(val url: String) : ShowdownLogProvider
    data class ReplayLog(val log: List<String>) : ShowdownLogProvider
}