package de.tectoast.emolga.domain.league.liveteam.service

import de.tectoast.emolga.domain.league.config.repository.LeagueConfigRepository
import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.liveteam.repository.LiveTeamRepository
import de.tectoast.emolga.domain.league.member.repository.LeagueMemberRepository
import de.tectoast.emolga.domain.league.teamgraphic.service.DynamicTeamGraphicService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.uuid.Uuid

@Single
class LiveTeamService(
    private val dynamicTeamGraphicService: DynamicTeamGraphicService,
    private val liveTeamRepo: LiveTeamRepository,
    private val leagueCoreRepo: LeagueCoreRepository,
    private val leagueMemberRepo: LeagueMemberRepository,
    private val leagueConfigRepo: LeagueConfigRepository,
) {
    suspend fun getLiveTeamGraphic(token: Uuid, numRaw: Int): ByteArray? {
        val leaguename = liveTeamRepo.getByCode(token) ?: return null
        val style = leagueConfigRepo.getConfig(leaguename).teamgraphics?.style ?: return null
        if (numRaw < 0) {
            if (style.individualBackgrounds) return null
            return withContext(Dispatchers.IO) {
                Files.readAllBytes(Path(style.backgroundPath(leaguename, -1)))
            }
        }
        val num = numRaw / 2
        val withMons = numRaw % 2
        val tableSize = leagueMemberRepo.getParticipantSize(leaguename)
        val roundIndex = (num / tableSize)
        val takePicks = roundIndex + withMons
        val indexInRound = num % tableSize
        val idx = leagueCoreRepo.getDraftRelevantData(leaguename, locking = false)?.draftOrder?.get(roundIndex + 1)
            ?.getOrNull(indexInRound) ?: return null
        return dynamicTeamGraphicService.getTeamGraphic(leaguename, idx, takePicks, blankBackground = true)
    }
}