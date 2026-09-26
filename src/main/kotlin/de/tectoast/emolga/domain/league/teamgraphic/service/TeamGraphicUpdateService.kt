package de.tectoast.emolga.domain.league.teamgraphic.service

import de.tectoast.emolga.domain.league.teamgraphic.repository.TeamGraphicRepository
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.koin.core.annotation.Single

@Single
class TeamGraphicUpdateService(
    private val teamGraphicRepo: TeamGraphicRepository,
    private val teamGraphicManager: TeamGraphicManager,
    baseScope: CoroutineScope
) {
    private val scope = baseScope + CoroutineName("TeamGraphicUpdateService")
    suspend fun updateTeamGraphic(messageId: Long): Unit? {
        val result = teamGraphicRepo.getLeagueAndIdxByMessageId(messageId)
            ?: return null
        scope.launch {
            val (leagueName, idx) = result
            teamGraphicManager.updateSingleTeamGraphic(leagueName, idx)
        }
        return Unit
    }
}