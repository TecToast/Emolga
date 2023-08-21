package de.tectoast.emolga.utils.draft

class WifiPlayer : DraftPlayer() {

    var alive = -1
    var winnerOfGame = false
    override val alivePokemon: Int
        get() = alive
    override val winner: Boolean
        get() = winnerOfGame
}
