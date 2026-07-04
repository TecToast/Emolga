package de.tectoast.emolga.domain.league.transaction.service

import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.domain.league.draft.repository.LeaguePickRepository
import de.tectoast.emolga.domain.league.transaction.repository.TransactionDataRepository
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.domain.pokemon.service.PokemonDisplayService
import de.tectoast.emolga.utils.Language
import de.tectoast.emolga.utils.coordXMod
import de.tectoast.emolga.utils.sheetupdate.SpreadsheetService
import de.tectoast.emolga.utils.y
import org.koin.core.annotation.Single

@Single
class TransactionExecutionService(
    private val spreadsheetService: SpreadsheetService,
    private val pokemonDisplayService: PokemonDisplayService,
    private val dataRepo: TransactionDataRepository,
    private val leagueCoreRepo: LeagueCoreRepository,
    private val leaguePickRepo: LeaguePickRepository,
) {
    suspend fun registerTransactions(leagueName: String, week: Int, onlyIndices: List<Int>?) {
        val leagueData = leagueCoreRepo.getScalarLeagueDataOrNull(leagueName) ?: return
        val weekData = dataRepo.getRunning(leagueName, week)
        if (weekData.isEmpty()) return
        val indices = onlyIndices ?: weekData.keys
        val displayNames = pokemonDisplayService.getDisplayNames(
            weekData.values.flatMapTo(mutableSetOf()) { it.drops + it.picks }, leagueData.guild, Language.ENGLISH
        )
        spreadsheetService.updateSheet(leagueData.sheetId, wait = true) {
            for (idx in indices) {
                val entry = weekData[idx] ?: continue
                val amountData = dataRepo.getTransactionAmounts(leagueName, idx)
                val picksOfUser = leaguePickRepo.getPicksForUser(leagueName, idx)
                val dropInsertIndex = amountData.mons - entry.drops.size
                addColumn(idx.coordXMod("Rosters", 5, 6, 3, 26, 22 + dropInsertIndex), entry.drops.map { displayNames[it] ?: it.value })
                for (newMon in entry.picks) {
                    val monIndex = picksOfUser.getIndexOfMon(newMon)
                    addSingle(
                        idx.coordXMod("Rosters", 5, 6, 3, 26, 10 + monIndex),
                        displayNames[newMon] ?: newMon.value
                    )
                }
                addColumn(idx.coordXMod("Rosters", 5, 6, 4, 26, 10), picksOfUser.filter { !it.quit }.map { it.tera })
                val batchGet = spreadsheetService.batchGet(leagueData.sheetId, entry.picks.map {
                    val monIndex = picksOfUser.getIndexOfMon(it)
                    val sourceY = idx.y(15, 3 + monIndex)
                    "Kills (type in)!I$sourceY:Q$sourceY"
                }, false).orEmpty()
                entry.drops.forEachIndexed { index, _ ->
                    if (batchGet[index] == null) return@forEachIndexed
                    val monIndex = picksOfUser.getIndexOfMon(entry.picks[index])
                    val sourceY = idx.y(15, 3 + monIndex)
                    val targetY = idx.y(15, 13 + dropInsertIndex + index)
                    addRow("Kills (type in)!I$sourceY", List(9) { "" })
                    addRow("Kills (type in)!I$targetY", batchGet[index]!![0]!!.map { it?.toString().orEmpty() })
                }
            }
        }
        dataRepo.removeRunning(leagueName, week, indices)
    }

    private fun List<DraftPokemon>.getIndexOfMon(monName: ShowdownID): Int {
        return indexOfFirst { it.showdownId == monName }.takeIf { it != -1 } ?: error("Mon $monName not found in picks")
    }
}
