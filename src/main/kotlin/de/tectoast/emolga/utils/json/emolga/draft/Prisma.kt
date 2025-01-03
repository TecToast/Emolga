package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.features.draft.during.BanMonCommand
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.records.SorterData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("Prisma")
class Prisma : League() {
    override val teamsize = 12
    override val pickBuffer = 5

    @Transient
    override val docEntry = DocEntry.create(this) {
        newSystem(SorterData("Tabelle!C4:F15", newMethod = true, cols = listOf(3, -1))) {
            b.addSingle(
                when (gdi) {
                    in 2..4 -> Coord("Spielplan", gdi.minus(2).x(4, 3), 10 + index)
                    else -> Coord("Spielplan", gdi.mod(5).x(4, 5), (gdi / 5).y(14, 3 + index))
                }, defaultGameplanString
            )
        }
    }

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        newSystemPickDoc(data.copy(round = convertToPickRound(data.round)))
        addPokemonToDraftorderSheet(data.pokemon)
        addSingle(data.idx.coordXMod("Teamseite", 4, 4, 3, 15, 4 + data.changedOnTeamsiteIndex), data.pokemon)
    }

    private fun RequestBuilder.addPokemonToDraftorderSheet(pokemon: String) {
        val r = round - 1
        val indexInRound = indexInRound(round)
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
                } + indexInRound
            ), pokemon
        )
    }

    fun RequestBuilder.banDoc(data: BanMonCommand.Args) {
        val pokemon = data.pokemon.tlName
        addPokemonToDraftorderSheet(pokemon)
        addSingle("Tierliste!G${bannedMons.size + 2}", pokemon)
    }

    companion object {
        val banRounds = setOf(1, 4, 6, 8, 10, 13, 15)
        val tierRanges = setOf(
            1..3 to "S",
            4..7 to "A",
            8..12 to "B",
            13..17 to "C",
            18..19 to "D",
        )

        fun convertToPickRound(r: Int) = r - banRounds.indexOfFirst { it > r }
    }
}
