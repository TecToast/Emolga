package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.commands.draft.PickData
import de.tectoast.emolga.commands.indexedBy
import de.tectoast.emolga.commands.x
import de.tectoast.emolga.commands.y
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.automation.structure.DocEntry
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.json.Emolga
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("ASL")
class ASL : League() {
    @Transient
    override val docEntry = DocEntry.create {

    }

    @Transient
    val comparator: Comparator<DraftPokemon> = compareBy({ it.tier.indexedBy(tierlist.order) }, { it.name })

    override fun pickDoc(data: PickData) {
        val b = builder()
        val asl = Emolga.get.asls11
        val (level, index, team) = asl.indexOfMember(data.mem)
        b.addSingle("Data$level!B${index.y(15, data.changedIndex + 3)}", data.pokemon)
        b.addColumn("$team!C${level.y(26, 23)}", data.picks.let { pi ->
            pi.sortedWith(comparator).map { it.indexedBy(pi) }.map { "=Data$level!B${index.y(15, 3) + it}" }
        })
        b.addSingle(
            "Draftreihenfolge ${
                when (level) {
                    0 -> "Coaches"
                    else -> "Stufe $level"
                }
            }!${data.round.minus(1).x(2, 2)}${data.indexInRound + 3}", data.pokemon
        )
        b.execute()
    }

    override fun announcePlayer() {
        tc.sendMessage("${getMention(current)} ist dran! (${points[current]} mögliche Punkte)")
            .queue()
    }

    override fun getMention(mem: Long) = "<@$mem> (<@&${Emolga.get.asls11.roleIdByMember(mem)}>)"

    override fun isCurrent(user: Long): Boolean {
        if (current == user || user == Constants.FLOID) return true
        return user in Emolga.get.asls11.teammembersByMember(current)
    }
}