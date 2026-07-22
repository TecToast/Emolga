package de.tectoast.emolga.domain.league.queue.service

import de.tectoast.emolga.domain.league.config.repository.LeagueConfigRepository
import de.tectoast.emolga.domain.league.draft.model.core.ValidationRelevantData
import de.tectoast.emolga.domain.league.draft.repository.LeaguePickRepository
import de.tectoast.emolga.domain.league.queue.model.QueuedAction
import de.tectoast.emolga.domain.league.tierlist.model.config.TierlistConfig
import de.tectoast.emolga.domain.league.tierlist.repository.TierlistRepository
import de.tectoast.emolga.domain.league.tierlist.service.action.dispatcher.TierlistActionDispatcher
import de.tectoast.emolga.utils.ErrorOrNull
import org.koin.core.annotation.Single

@Single
class QueueValidationService(
    private val picksRepo: LeaguePickRepository,
    private val tierlistActionDispatcher: TierlistActionDispatcher,
    private val tierlistRepo: TierlistRepository,
    private val leagueConfigRepo: LeagueConfigRepository
) {
    suspend fun validateQueue(leagueName: String, guild: Long, idx: Int, list: List<QueuedAction>): ErrorOrNull {
        val config = leagueConfigRepo.getConfig(leagueName)
        val meta = tierlistRepo.getMeta(guild, config.tlIdentifier)!!
        return validateQueue(
            leagueName, idx, meta.teamSize, meta.config, list
        )
    }

    suspend fun validateQueue(
        leagueName: String,
        idx: Int,
        teamSize: Int,
        tierlistConfig: TierlistConfig,
        list: List<QueuedAction>
    ) =
        with(ValidationRelevantData(picksRepo.getPicksForUser(leagueName, idx), idx, teamSize)) {
            tierlistActionDispatcher.checkLegalityOfQueue(tierlistConfig, idx, list)
        }
}