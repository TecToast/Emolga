package de.tectoast.emolga.domain.userdata.service

import de.tectoast.emolga.discord.DiscordUserData
import de.tectoast.emolga.discord.DiscordUserProvider
import de.tectoast.emolga.domain.userdata.repository.DiscordUserCacheRepository
import org.koin.core.annotation.Single

@Single
class DiscordUserService(
    private val repo: DiscordUserCacheRepository,
    private val discordUserProvider: DiscordUserProvider
) {

    suspend fun getData(guildId: Long, users: Iterable<Long>): Map<Long, DiscordUserData> {
        val validCache = repo.getValidEntries(users)
        val usersToFetch = users.toSet() - validCache.mapTo(mutableSetOf()) { it.userId }
        val fetched = if(usersToFetch.isNotEmpty()) {
            val fetched = discordUserProvider.provideMultipleUsers(guildId, usersToFetch)
            repo.set(fetched)
            fetched
        } else emptyMap()
        return buildMap {
            validCache.forEach { put(it.userId, it) }
            putAll(fetched)
        }
    }

    suspend fun getNames(guildId: Long, users: Iterable<Long>): Map<Long, String> {
        return getData(guildId, users).mapValues { it.value.displayName }
    }

}
