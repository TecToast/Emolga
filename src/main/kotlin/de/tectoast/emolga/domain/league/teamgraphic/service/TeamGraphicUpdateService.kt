package de.tectoast.emolga.domain.league.teamgraphic.service

import de.tectoast.emolga.domain.league.teamgraphic.repository.TeamGraphicRepository
import de.tectoast.emolga.utils.createCoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single
class TeamGraphicUpdateService(
    private val teamGraphicRepo: TeamGraphicRepository,
    private val teamGraphicGenerator: TeamGraphicGenerator,
    dispatcher: CoroutineDispatcher
) {
    private val scope = createCoroutineScope("TeamGraphicUpdateService", dispatcher)
    suspend fun updateTeamGraphic(messageId: Long): Unit? {
        val result = teamGraphicRepo.getLeagueAndIdxByMessageId(messageId)
            ?: return null
        scope.launch {
            val (leagueName, idx) = result
            teamGraphicGenerator.editTeamGraphicForLeague(leagueName, idx)
        }
        return Unit
    }
}