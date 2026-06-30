package de.tectoast.emolga.domain.league.member.service

import de.tectoast.emolga.discord.DiscordUserData
import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.member.repository.LeagueMemberRepository
import de.tectoast.emolga.domain.userdata.service.DiscordUserService
import org.koin.core.annotation.Single

@Single
class LeagueParticipantDataService(
    private val leagueMemberRepo: LeagueMemberRepository,
    private val leagueCoreRepo: LeagueCoreRepository,
    private val discordUserService: DiscordUserService
) {
    suspend fun getLeagueParticipantData(guild: Long, leagueName: String): List<List<DiscordUserData>>? {
        val leagueData = leagueCoreRepo.getScalarLeagueDataOrNull(leagueName) ?: return null
        if (leagueData.guild != guild) return null
        val primaryIds = leagueMemberRepo.getPrimaryIds(leagueName)
        val userData = discordUserService.getData(guild, primaryIds.flatMap { it.value })
        return primaryIds.map { entry -> entry.value.mapNotNull { userData[it] } }
    }
}
