package de.tectoast.emolga.domain.league.result.service

import de.tectoast.emolga.discord.DiscordUserProvider
import de.tectoast.emolga.discord.GuildMetaRepository
import de.tectoast.emolga.domain.league.config.repository.LeagueConfigRepository
import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.draft.repository.LeaguePickRepository
import de.tectoast.emolga.domain.league.result.model.ResultCodePokemon
import de.tectoast.emolga.domain.league.result.model.ResultCodeResponse
import de.tectoast.emolga.domain.league.result.model.ResultUserData
import de.tectoast.emolga.domain.league.result.repository.ResultCodesRepository
import de.tectoast.emolga.domain.league.tierlist.repository.TierlistRepository
import de.tectoast.emolga.domain.league.tierlist.service.action.dispatcher.TierlistActionDispatcher
import de.tectoast.emolga.domain.pokemon.repository.PokedexRepository
import de.tectoast.emolga.domain.pokemon.service.PokemonDisplayService
import org.koin.core.annotation.Single

@Single
class ResultSetupService(
    private val repository: ResultCodesRepository,
    private val leagueCoreRepo: LeagueCoreRepository,
    private val leaguePickRepo: LeaguePickRepository,
    private val discordUserProvider: DiscordUserProvider,
    private val leagueConfigRepo: LeagueConfigRepository,
    private val tierlistRepo: TierlistRepository,
    private val guildMetaRepo: GuildMetaRepository,
    private val tierlistActionDispatcher: TierlistActionDispatcher,
    private val pokemonDisplayService: PokemonDisplayService,
    private val pokedexRepo: PokedexRepository,
) {
    suspend fun getResultDataForUser(resultid: String): ResultCodeResponse? {
        val entry = repository.getEntryByCode(resultid) ?: return null
        val leagueName = entry.leagueName
        val league = leagueCoreRepo.getLeagueWithParticipants(leagueName) ?: return null
        val config = leagueConfigRepo.getConfig(leagueName)
        val gid = league.guild
        val idxes = listOf(entry.p1, entry.p2)
        val users = league.users
        val memberData = discordUserProvider.provideMultipleUsers(gid, idxes.map { users[it] })
        val tlMeta = tierlistRepo.getMeta(gid, config.tlIdentifier) ?: return null
        val allPicks = idxes.map { leaguePickRepo.getPicksForUser(leagueName, it) }
        val allShowdownIds = allPicks.flatten().map { it.showdownId }
        val displayNames =
            pokemonDisplayService.getDisplayNames(allShowdownIds, gid, tlMeta.language)
        val pokedex = pokedexRepo.getAll(allShowdownIds)
        return ResultCodeResponse(
            guildName = guildMetaRepo.getGuildName(gid) ?: "Unknown guild",
            logoUrl = guildMetaRepo.getGuildIcon(gid),
            bo3 = config.triggers.bo3,
            week = entry.week,
            data = idxes.mapIndexed { index, idx ->
                val picks = allPicks[index]
                val uid = users[idx]
                val member = memberData[uid]!!
                val avatarUrl = member.avatarUrl
                ResultUserData(
                    name = member.displayName,
                    avatarUrl = avatarUrl,
                    picks = picks.sortedWith(tierlistActionDispatcher.getTierOrderingComparatorWithoutName(tlMeta.config))
                        .map {
                            val displayName = displayNames[it.showdownId]!!
                            ResultCodePokemon(
                                displayName, pokedex[it.showdownId]!!.calcSpriteName()
                            )
                        })
            })
    }


}