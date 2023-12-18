package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.commands.draft.during.BanCommandArgs
import de.tectoast.emolga.commands.x
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.records.Coord
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Prisma")
class Prisma : League() {
    override val teamsize = 11

    val bannedMons: MutableSet<String> = mutableSetOf()

    override fun beforePick(): String? {
        return "Dies ist eine Ban-Runde!".takeIf { round in banRounds }
    }

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        newSystemPickDoc(data.copy(round = convertToPickRound(data.round)))
        addPokemonToDoc(data.pokemon)
    }

    private fun RequestBuilder.addPokemonToDoc(pokemon: String) {
        val r = round - 1
        addSingle(
            Coord(
                "Draftreihenfolge", when (r) {
                    in 0..2 -> r.x(2, 3)
                    in 3..6 -> r.minus(3).x(2, 3)
                    in 7..11 -> r.minus(7).x(2, 3)
                    in 12..16 -> r.minus(12).x(2, 3)
                    else -> r.minus(17).x(2, 3)
                }, when (r) {
                    in 0..2 -> 5
                    in 3..6 -> 16
                    in 7..11 -> 27
                    in 12..16 -> 38
                    else -> 49
                }
            ), pokemon
        )
    }

    fun RequestBuilder.banDoc(data: BanCommandArgs) {
        val pokemon = data.pokemon.tlName
        addPokemonToDoc(pokemon)
        addSingle("Tierliste!G${bannedMons.size + 2}", pokemon)
    }

    companion object {
        val banRounds = setOf(1, 4, 6, 8, 10, 13, 15)
        fun convertToPickRound(r: Int) = r - banRounds.indexOfFirst { it > r }
    }
}
