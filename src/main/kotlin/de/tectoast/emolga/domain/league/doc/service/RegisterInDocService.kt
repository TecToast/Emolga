package de.tectoast.emolga.domain.league.doc.service

import de.tectoast.emolga.domain.league.gamedata.repository.GameDataRepository
import org.koin.core.annotation.Single

@Single
class RegisterInDocService(
    private val replayDataRepo: GameDataRepository,
    private val docEntryService: DocEntryService
) {
    suspend fun registerInDoc(leagueName: String, week: Int, battleIndex: Int) {
        val fullGameData = replayDataRepo.getFullGameData(leagueName, week, battleIndex) ?: return
        docEntryService.process(leagueName, fullGameData)
    }
}
