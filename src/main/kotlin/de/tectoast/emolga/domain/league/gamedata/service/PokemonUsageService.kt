package de.tectoast.emolga.domain.league.gamedata.service

import de.tectoast.emolga.domain.league.config.repository.LeagueConfigRepository
import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.gamedata.model.UsageData
import de.tectoast.emolga.domain.league.gamedata.model.UsageDataTotal
import de.tectoast.emolga.domain.league.gamedata.repository.GameDataRepository
import de.tectoast.emolga.domain.league.tierlist.repository.TierlistRepository
import de.tectoast.emolga.domain.pokemon.service.PokemonDisplayService
import de.tectoast.emolga.utils.Language
import org.koin.core.annotation.Single

@Single
class PokemonUsageService(
    private val leagueCoreRepo: LeagueCoreRepository,
    private val replayDataRepo: GameDataRepository,
    private val pokemonDisplayService: PokemonDisplayService,
    private val leagueConfigRepo: LeagueConfigRepository,
    private val tierlistRepo: TierlistRepository,
) {
    suspend fun getUsageDataTotal(leagueName: String, maxWeek: Int?): UsageDataTotal? {
        val leagueData = leagueCoreRepo.getScalarLeagueDataOrNull(leagueName) ?: return null
        val allLeagues = leagueCoreRepo.getLeagueNamesByGuild(leagueData.guild)
        val allGameDatas = replayDataRepo.getAllGameDataUntil(leagueName, maxWeek)
        val actualMaxWeek = allGameDatas.maxOfOrNull { it.second } ?: 1
        val eachCount =
            allGameDatas.flatMap { replayData -> replayData.first.kd.flatten().map { it.name } }.groupingBy { it }
                .eachCount()
        val tlIdentifier = leagueConfigRepo.getConfig(leagueName).tlIdentifier
        val language = tierlistRepo.getMeta(leagueData.guild, tlIdentifier)?.language ?: Language.ENGLISH
        val allDisplayNames = pokemonDisplayService.getDisplayNames(eachCount.keys, leagueData.guild, language)
        val data =
            eachCount.map { (showdownID, count) -> UsageData(allDisplayNames[showdownID] ?: showdownID.value, count) }
                .sortedWith(compareByDescending<UsageData> { it.count }.thenBy { it.mon })
        return UsageDataTotal(allGameDatas.size, actualMaxWeek, allLeagues, data)
    }
}