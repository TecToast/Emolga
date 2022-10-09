package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.coordXMod
import de.tectoast.emolga.utils.DraftTimer
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.automation.structure.BasicResultCreator
import de.tectoast.emolga.utils.automation.structure.BasicStatProcessor
import de.tectoast.emolga.utils.automation.structure.CombinedStatProcessor
import de.tectoast.emolga.utils.automation.structure.DocEntry
import de.tectoast.emolga.utils.records.StatLocation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("DoR")
class DoR : League() {
    @Transient
    override val docEntry = DocEntry.create {
        league = this@DoR
        killProcessor = BasicStatProcessor { plindex, monindex, gameday ->
            StatLocation("Kader", plindex % 2 * 14 + 5 + gameday, plindex / 2 * 17 + 6 + monindex)
        }
        deathProcessor = CombinedStatProcessor { plindex, gameday ->
            StatLocation("Kader", plindex % 2 * 14 + 5 + gameday, plindex / 2 * 17 + 18)
        }
        resultCreator =
            BasicResultCreator { b: RequestBuilder, gdi: Int, index: Int, numberOne: Int, numberTwo: Int, url: String ->
                b.addRow(
                    "Spielplan!${Command.getAsXCoord(gdi / 5 * 6 + 3)}${gdi % 5 * 10 + 7 + index + (index / 2)}",
                    listOf(numberOne, "=HYPERLINK(\"$url\"; \":\")", numberTwo)
                )
            }
        monsOrder = { l -> l.sortedWith(compareBy({ it.free }, { if (it.free) "" else it.tier })).map { it.name } }
    }
    override val timer = DraftTimer.DoR

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