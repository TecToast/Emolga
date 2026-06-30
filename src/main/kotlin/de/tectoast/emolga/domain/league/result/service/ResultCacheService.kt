package de.tectoast.emolga.domain.league.result.service

import de.tectoast.emolga.domain.league.core.model.LeagueWithParticipants
import de.tectoast.emolga.domain.userdata.service.DiscordUserService
import org.koin.core.annotation.Single


@Single
class ResultCacheService(private val discordUserService: DiscordUserService) {
    private val nameCache = mutableMapOf<String, Map<Long, String>>()
    private val leagueCache = mutableMapOf<Long, String>()

    fun setLeague(uid: Long, league: String) {
        leagueCache[uid] = league
    }

    suspend fun getNamesOrFetch(league: LeagueWithParticipants): Collection<String> {
        return nameCache.getOrPut(league.leagueName) {
            discordUserService.getNames(league.guild, league.users)
        }.values
    }

    fun resolveOpponent(user: Long, opponentName: String): Long? {
        return nameCache[leagueCache[user]]?.reverseGet(opponentName)
    }

    private fun <T, R> Map<T, R>.reverseGet(value: R): T? = this.entries.firstOrNull { it.value == value }?.key
}
