package de.tectoast.emolga.features.league.draft.queue

import de.tectoast.emolga.domain.league.queue.service.QueuePicksService
import de.tectoast.emolga.domain.league.util.service.LeagueQueryService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.league.draft.generic.K18n_NoLeagueForGuildFound
import de.tectoast.emolga.utils.msg
import org.koin.core.annotation.Single

@Single
class QueueActivationHelper(
    private val leagueQueryService: LeagueQueryService,
    private val queuePicksService: QueuePicksService
) {
    context(iData: InteractionData)
    suspend fun changeActivation(enable: Boolean) {
        val (leagueName, config, idx) = leagueQueryService.byCommand() ?: return iData.reply(K18n_NoLeagueForGuildFound)
        val result = queuePicksService.changeActivation(enable, iData.gid, leagueName, idx, config)
        iData.reply(result.msg(), ephemeral = true)
    }

    context(iData: InteractionData)
    suspend fun toggleSuccessfulPing() {
        val (leagueName, config, idx) = leagueQueryService.byCommand() ?: return iData.reply(K18n_NoLeagueForGuildFound)
        val result = queuePicksService.toggleSuccessfulNotification( iData.gid, leagueName, idx, config)
        iData.reply(result.msg(), ephemeral = true)
    }
}
