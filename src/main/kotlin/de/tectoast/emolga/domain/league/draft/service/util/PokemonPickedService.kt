package de.tectoast.emolga.domain.league.draft.service.util

import de.tectoast.emolga.di.StartupTask
import de.tectoast.emolga.domain.league.config.repository.LeagueConfigRepository
import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.draft.model.util.DivisionPickedData
import de.tectoast.emolga.domain.league.draft.model.util.PokemonPickedData
import de.tectoast.emolga.domain.league.draft.repository.LeaguePickRepository
import de.tectoast.emolga.domain.league.draft.service.core.PicksModifiedFlow
import de.tectoast.emolga.domain.league.tierlist.repository.TierlistRepository
import de.tectoast.emolga.domain.pokemon.repository.PokedexRepository
import de.tectoast.emolga.domain.pokemon.service.PokemonDisplayService
import de.tectoast.emolga.utils.Language
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.newThreadSafeCache
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.Single

@Single
class PokemonPickedService(
    private val leagueCoreRepo: LeagueCoreRepository,
    private val leagueConfigRepo: LeagueConfigRepository,
    private val tierlistRepo: TierlistRepository,
    private val leaguePickRepo: LeaguePickRepository,
    private val pokemonDisplayService: PokemonDisplayService,
    private val pokedexRepo: PokedexRepository,
    private val picksModifiedFlow: PicksModifiedFlow,
    dispatcher: CoroutineDispatcher
) : StartupTask {
    private val pickedDataCache = newThreadSafeCache<Long, List<PokemonPickedData>>()
    private val scope = createCoroutineScope("PokemonPickedService", dispatcher)

    override suspend fun onStartup() {
        picksModifiedFlow.launch(scope) { pickedDataCache.remove(it) }
    }

    suspend fun getPokemonPickedData(guild: Long): List<PokemonPickedData> = pickedDataCache.getOrPut(guild) {
        val allLeagueData = leagueCoreRepo.getAllScalarLeagueData(guild)
        val allLeagueNames = allLeagueData.map { it.leagueName }
        val firstData = allLeagueData.firstOrNull() ?: return@getOrPut emptyList()
        val language =
            tierlistRepo.getMeta(guild, leagueConfigRepo.getConfig(firstData.leagueName).tlIdentifier)?.language
                ?: Language.ENGLISH
        val allPicksByLeague = leaguePickRepo.getAllCurrentPicksInLeagues(allLeagueNames)
        val allEntries = allPicksByLeague.flatMap { (league, picks) -> picks.map { league to it } }
        val lookUp = allEntries.groupBy { it.second.showdownId }
        val allDisplayNames = pokemonDisplayService.getDisplayNames(lookUp.keys, guild, language)
        val leagueNameToDisplayName = allLeagueData.associate { it.leagueName to it.displayName }
        val leagueNameToNum = allLeagueData.associate { it.leagueName to it.num }
        lookUp.map { (showdownID, value) ->
            val displayName = allDisplayNames[showdownID] ?: showdownID.value
            val amount = value.size
            val tier = value.first().second.tier
            val spriteName = pokedexRepo.get(showdownID)?.calcSpriteName() ?: showdownID.value
            PokemonPickedData(
                name = displayName, tier = tier, divs = value
                    .sortedWith(compareBy({ leagueNameToNum[it.first] ?: Int.MAX_VALUE }, { it.first }))
                    .map { v ->
                        DivisionPickedData(
                            leagueNameToDisplayName[v.first] ?: v.first, v.second.tera
                        )
                    }, spriteName = spriteName, amount = amount
            )
        }.sortedByDescending { it.amount }
    }
}