package de.tectoast.emolga.domain.league.teamgraphic.service

import de.tectoast.emolga.domain.league.liveteam.repository.LiveTeamRepository
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Single
class SingleTeamGraphicService(
    private val liveTeamRepo: LiveTeamRepository,
    private val dynamicTeamGraphicService: DynamicTeamGraphicService
) {
    suspend fun getSingleTeamGraphic(token: Uuid, idx: Int, takePicks: Int?): ByteArray? {
        val leagueName = liveTeamRepo.getByCode(token) ?: return null
        return dynamicTeamGraphicService.getTeamGraphic(leagueName, idx, takePicks, blankBackground = false)
    }
}
