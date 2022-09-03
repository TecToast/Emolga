package de.tectoast.emolga.buttons.buttonsaves

class PrismaTeam(private val mons: List<String>, val index: Int) {
    private var x = 0
    fun nextMon(): PokemonData {
        return (x++).let {
            PokemonData(mons[it], 13 - it)
        }
    }

    class PokemonData(val pokemon: String, val ycoord: Int)
}