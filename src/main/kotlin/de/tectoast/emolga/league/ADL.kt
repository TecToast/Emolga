package de.tectoast.emolga.league

import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.league.config.TransactionAmounts
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.records.CoordXMod
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("ADL")
class ADL : League() {
    override val teamsize = 10

    @Transient
    override val docEntry = DocEntry.create(this) {
        +StatProcessor {
            Coord("Kills (type in)", 9 + gdi, memIdx.y(15, 3 + monIndex())) to DataTypeForMon.KILLS
        }
        resultCreator = {
            b.addRow(
                gdi.CoordXMod("Schedule", 3, 8, 4, 7, 3 + index), buildDefaultSplitGameplanString("vs.")
            )
        }
    }

    override suspend fun executeTransactionDocInsert(
        gameday: Int, onlyIndices: List<Int>?
    ) {
        val transaction = persistentData.transaction
        val amounts = transaction.amounts
        val gamedayData = transaction.running[gameday] ?: return
        val indices = onlyIndices ?: gamedayData.keys
        val b = builder()
        for (idx in indices) {
            val entry = gamedayData[idx] ?: continue
            val amountData = amounts.getOrPut(idx) { TransactionAmounts() }
            val dropInsertIndex = amountData.mons - entry.drops.size
            b.addColumn(idx.CoordXMod("Rosters", 5, 6, 3, 26, 22 + dropInsertIndex), entry.drops)
            val picksOfUser = picks(idx)
            val officialToTL = NameConventionsDB.convertAllOfficialToTL(entry.picks, guild)
            for (newMon in entry.picks) {
                val monIndex = picksOfUser.getIndexOfMon(newMon)
                b.addSingle(idx.CoordXMod("Rosters", 5, 6, 3, 26, 10 + monIndex), officialToTL[newMon]!!)
            }
            b.addColumn(idx.CoordXMod("Rosters", 5, 6, 4, 26, 10), picksOfUser.filter { !it.quit }.map { it.tera })
            val batchGet = Google.batchGet(sid, entry.picks.map {
                val monIndex = picksOfUser.getIndexOfMon(it)
                val sourceY = idx.y(15, 3 + monIndex)
                "Kills (type in)!I$sourceY:Q$sourceY"
            }, false).orEmpty()
            entry.drops.forEachIndexed { index, drop ->
                @Suppress("SENSELESS_COMPARISON")
                if (batchGet[index] == null) return@forEachIndexed
                val monIndex = picksOfUser.getIndexOfMon(entry.picks[index])
                val sourceY = idx.y(15, 3 + monIndex)
                val targetY = idx.y(15, 13 + dropInsertIndex + index)
                b.addRow("Kills (type in)!I$sourceY", List(9) { "" })
                b.addRow("Kills (type in)!I$targetY", batchGet[index][0].map { it?.toString().orEmpty() })
            }
            gamedayData.remove(idx)
        }
        b.executeAndWait()
        save()
    }

    private fun List<DraftPokemon>.getIndexOfMon(monName: String): Int {
        return indexOfFirst { it.name == monName }.takeIf { it != -1 } ?: error("Mon $monName not found in picks")
    }
}