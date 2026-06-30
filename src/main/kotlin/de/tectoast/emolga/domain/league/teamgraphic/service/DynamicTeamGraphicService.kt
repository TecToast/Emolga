package de.tectoast.emolga.domain.league.teamgraphic.service

import de.tectoast.emolga.domain.league.config.repository.LeagueConfigRepository
import de.tectoast.emolga.domain.league.draft.repository.LeaguePickRepository
import de.tectoast.emolga.utils.newThreadSafeCache
import org.koin.core.annotation.Single
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Single
class DynamicTeamGraphicService(
    private val leaguePickRepo: LeaguePickRepository,
    private val teamGraphicGenerator: TeamGraphicGenerator,
    private val teamDataCreationService: TeamDataCreationService,
    private val leagueConfigRepo: LeagueConfigRepository,
) {
    private val teamGraphicCache = newThreadSafeCache<String, ByteArray>(maxSize = 100)

    suspend fun getTeamGraphic(leagueName: String, idx: Int, takePicks: Int?, blankBackground: Boolean): ByteArray? {
        val picks = leaguePickRepo.getPicksForUser(leagueName, idx)
        val actualPickSize = picks.size
        if (takePicks != null && takePicks > actualPickSize) return null
        val actualTakePicks = takePicks ?: actualPickSize
        return teamGraphicCache.getOrPut("${leagueName}#$idx#$actualTakePicks#$blankBackground") {
            val img = teamGraphicGenerator.generate(
                teamDataCreationService.singleFromLeagueUnordered(leagueName, idx, picks.take(actualTakePicks)),
                leagueConfigRepo.getConfig(leagueName).teamgraphics?.style ?: return null,
                TeamGraphicGenerator.Options(blankBackground)
            )
            ByteArrayOutputStream().use {
                ImageIO.write(img, "png", it)
                it.toByteArray()
            }
        }
    }
}
