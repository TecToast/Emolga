package de.tectoast.emolga.domain.league.teamgraphic.service

import de.tectoast.emolga.di.StartupTask
import de.tectoast.emolga.domain.eventbus.EventBus
import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.signup.model.LogoChangedEvent
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
    private val leagueCoreRepo: LeagueCoreRepository,
    private val eventBus: EventBus,
    baseScope: CoroutineScope
) : StartupTask {
    private val scope = baseScope + CoroutineName("TeamGraphicUpdateService")

    override suspend fun onStartup() {
        eventBus.collect<LogoChangedEvent>(scope) {
            leagueCoreRepo.getLeagueNameAndIdxByGuildUser(it.guild, it.userId)?.let { (leagueName, idx) ->
                teamGraphicManager.updateSingleTeamGraphic(leagueName, idx)
            }
        }
    }

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