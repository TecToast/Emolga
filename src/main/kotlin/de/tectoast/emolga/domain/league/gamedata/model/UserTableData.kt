package de.tectoast.emolga.domain.league.gamedata.model


data class UserTableData(
    var points: Int = 0, var kills: Int = 0, var deaths: Int = 0, var wins: Int = 0, var losses: Int = 0, val index: Int
) {
    val diff get() = kills - deaths
}