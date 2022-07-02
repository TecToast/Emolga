package de.tectoast.emolga.buttons.buttonsaves

class PrismaTeam(private val mons: List<String>, val index: Int) {
    private var x = 0
    fun nextMon(): PokemonData {
        val i = x++
        return PokemonData(mons[i], 13 - i)
    }

    inner class PokemonData(val pokemon: String, val ycoord: Int)
}