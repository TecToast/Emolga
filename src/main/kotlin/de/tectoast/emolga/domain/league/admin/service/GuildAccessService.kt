package de.tectoast.emolga.domain.league.admin.service

import de.tectoast.emolga.domain.league.admin.repository.GuildManagerRepository
import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.utils.BotConstants
import org.koin.core.annotation.Single

@Single
class GuildAccessService(
    private val repo: GuildManagerRepository,
    private val leagueCoreRepo: LeagueCoreRepository,
    private val botConstants: BotConstants
) {
    /**
     * Gets all guilds the user is authorized for
     * @return the list containing the guild ids
     */
    suspend fun getGuildsForUser(user: Long): Set<Long> {
        val result = repo.getDirectlyAuthorizedGuilds(user)
        if (user == botConstants.botOwnerId) {
            return result + leagueCoreRepo.getAllLeagueGuilds() + botConstants.botOwnerGuildId
        }
        return result
    }
}