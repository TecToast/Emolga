package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.coordXMod
import de.tectoast.emolga.commands.draft.PickData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("DoR")
class DoR : League() {
    @Transient
    override val docEntry = null

    override fun pickDoc(data: PickData) {
        val b = builder()
        b.addSingle(data.memIndex.coordXMod("Kader",
            2,
            14,
            4,
            17,
            if (data.freePick) data.picks.count { it.free } + 13 else getTierInsertIndex(data.picks, data.tier) + 6),
            data.pokemon)
        b.addSingle(data.round.minus(1).coordXMod("Draftreihenfolge", 4, 4, 4, 13, 3 + data.indexInRound), data.pokemon)
        if (data.freePick) b.addSingle(
            data.round.minus(1).coordXMod("Draftreihenfolge", 4, 4, 5, 13, 3 + data.indexInRound), "F"
        )
        b.execute()
    }

    override fun handleTiers(e: GuildCommandEvent, tier: String, origtier: String): Boolean {
        if (tierlist.mode.isPoints()) return false
        val map = getPossibleTiers()
        if (!map.containsKey(tier)) {
            e.reply("Das Tier `$tier` existiert nicht!")
            return true
        }
        if (tierlist.order.indexOf(origtier) < tierlist.order.indexOf(tier)) {
            e.reply("Du kannst ein Tier-$origtier-Mon nicht ins $tier. Tier hochdraften!")
            return true
        }
        if (map[tier]!! <= 0) {
            if (tierlist.prices[tier] == 0) {
                e.reply("Ein Pokemon aus Tier $tier musst du in ein anderes Tier hochdraften!")
                return true
            }
            e.reply("Du kannst dir kein Tier-$tier-Pokemon mehr picken!")
            return true
        }
        return false
    }

    override fun afterPick() {
        if (hasMovedTurns()) {
            announcePlayer()
            movedTurns().removeFirstOrNull()
        } else nextPlayer()
    }

    override fun getPickRound(): Int {
        return movedTurns().firstOrNull() ?: round
    }
}