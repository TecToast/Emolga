package de.tectoast.emolga.utils

import de.tectoast.emolga.utils.showdown.Player

fun interface ReplayAnalyser {
    fun analyse(
        game: Array<Player>,
        uid1: String,
        uid2: String,
        kills: List<Map<String, String>>,
        deaths: List<Map<String, String>>,
        vararg optionalArgs: Any?
    )
}