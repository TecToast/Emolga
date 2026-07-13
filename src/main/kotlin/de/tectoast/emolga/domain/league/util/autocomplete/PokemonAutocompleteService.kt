package de.tectoast.emolga.domain.league.util.autocomplete

import de.tectoast.emolga.domain.league.config.repository.LeagueConfigRepository
import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.draft.model.core.PickContext
import de.tectoast.emolga.domain.league.draft.repository.LeaguePickRepository
import de.tectoast.emolga.domain.league.draft.service.util.DraftCurrentService
import de.tectoast.emolga.domain.league.tierlist.repository.TierlistRepository
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.domain.pokemon.model.ShowdownIDWithDisplayName
import de.tectoast.emolga.domain.pokemon.service.PokemonDisplayService
import de.tectoast.emolga.utils.getOrReturn
import de.tectoast.emolga.utils.newThreadSafeCache
import org.koin.core.annotation.Single

private typealias CacheKey = Pair<Long, String> // guildId, tlIdentifier

private data class CacheValue(val showdownId: ShowdownID, val displayNames: List<String>)


@Single
class PokemonAutocompleteService(
    private val leagueCoreRepo: LeagueCoreRepository,
    private val tierlistRepo: TierlistRepository,
    private val leagueConfigRepo: LeagueConfigRepository,
    private val displayService: PokemonDisplayService,
    private val leaguePickRepo: LeaguePickRepository,
    private val draftCurrentService: DraftCurrentService
) {
    private val cache = newThreadSafeCache<CacheKey, List<CacheValue>>(10)
    suspend fun autocompletePokemon(query: String, guild: Long, channel: Long, user: Long, limit: Int): List<String>? {
        val leagueWithGuild = leagueCoreRepo.getLeagueFromDraftChannelOrUser(channel, guild, user)
        val realGuild = leagueWithGuild?.second ?: guild
        val cacheKey = leagueWithGuild?.let { (leagueName, realGuild) ->
            CacheKey(realGuild, leagueConfigRepo.getConfig(leagueName).tlIdentifier)
        } ?: CacheKey(realGuild, "")
        val rawResult = cache.getOrPut(cacheKey) {
            val allSDIds = tierlistRepo.getAllShowdownIds(cacheKey.first, cacheKey.second)
            displayService.getAutocompleteNames(allSDIds, realGuild).map { CacheValue(it.key, it.value) }
        }
        val filtered = mutableListOf<ShowdownIDWithDisplayName>()
        outer@ for (value in rawResult) {
            for (displayName in value.displayNames) {
                if (displayName.contains(query, ignoreCase = true)) {
                    filtered += ShowdownIDWithDisplayName(value.showdownId, displayName)
                    if (filtered.size > limit) break@outer
                }
            }
        }
        if (filtered.size > limit) return null
        val finalStrings = if (leagueWithGuild == null) filtered.mapTo(mutableSetOf()) { it.displayName } else {
            val allShowdownIds = leaguePickRepo.getAllPickedIds(leagueWithGuild.first)
            filtered.mapTo(mutableSetOf()) { if (allShowdownIds.contains(it.showdownId)) "${it.displayName} (NICHT VERFÜGBAR)" else it.displayName }
        }
        return finalStrings.sortedWith(compareBy({ !it.startsWith(query) }, { it }))
    }

    suspend fun autocompletePokemonOfTeam(query: String, guild: Long, channel: Long, user: Long): List<String>? {
        // TODO: extend for not draft channel
        val leagueData = leagueCoreRepo.getDraftRelevantData(channel, locking = false) ?: return null
        val idx =
            draftCurrentService.getCurrentUser(leagueData, user).getOrReturn<PickContext, Unit> { return null }.idx
        val picks = leaguePickRepo.getPicksForUser(leagueData.leagueName, idx)
        val showdownIds = picks.map { it.showdownId }
        val names = displayService.getAutocompleteNames(showdownIds, leagueData.guild)
        return showdownIds.flatMap { names[it].orEmpty() }.filter { it.contains(query, ignoreCase = true) }
    }
}
