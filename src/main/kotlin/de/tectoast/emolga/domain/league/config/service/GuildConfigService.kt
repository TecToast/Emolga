package de.tectoast.emolga.domain.league.config.service

import de.tectoast.emolga.domain.league.config.model.LeagueConfig
import de.tectoast.emolga.domain.league.config.repository.LeagueConfigRepository
import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import org.koin.core.annotation.Single

@Single
class GuildConfigService(
    private val leagueCoreRepo: LeagueCoreRepository,
    private val leagueConfigRepo: LeagueConfigRepository
) {
    suspend fun getAllConfigsForGuild(guild: Long): List<LeagueConfig> {
        return leagueCoreRepo.getLeagueNamesByGuild(guild).map { leagueConfigRepo.getConfig(it) }
    }
}
