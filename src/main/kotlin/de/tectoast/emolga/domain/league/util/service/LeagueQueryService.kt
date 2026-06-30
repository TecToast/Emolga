package de.tectoast.emolga.domain.league.util.service

import de.tectoast.emolga.domain.game.service.process.analysis.BattleContext
import de.tectoast.emolga.domain.game.service.process.analysis.SDPlayer
import de.tectoast.emolga.domain.league.config.repository.LeagueConfigRepository
import de.tectoast.emolga.domain.league.core.repository.LeagueCoreTable
import de.tectoast.emolga.domain.league.draft.repository.LeaguePickTable
import de.tectoast.emolga.domain.league.member.repository.LeagueUserTable
import de.tectoast.emolga.domain.league.util.model.LeagueQueryResult
import de.tectoast.emolga.domain.league.util.model.LeagueResult
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.domain.pokemon.repository.PokedexRepository
import de.tectoast.emolga.utils.groupByMapping
import de.tectoast.emolga.utils.toShowdownID
import kotlinx.coroutines.flow.*
import mu.KotlinLogging
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single
import kotlin.time.measureTimedValue

@Single
class LeagueQueryService(
    private val db: R2dbcDatabase,
    private val configRepository: LeagueConfigRepository,
    private val pokedexRepo: PokedexRepository
) {
    private val logger = KotlinLogging.logger {}
    suspend fun getByGuildUser(guild: Long, user: Long) = suspendTransaction(db) {
        LeagueCoreTable.innerJoin(LeagueUserTable, { leagueName }, { leagueName })
            .select(LeagueCoreTable.leagueName, LeagueCoreTable.configOverride, LeagueUserTable.idx)
            .where { (LeagueCoreTable.guild eq guild) and (LeagueUserTable.userId eq user) and (LeagueUserTable.substitute eq false) }
            .firstOrNull()?.let {
                val leagueConfig = it[LeagueCoreTable.configOverride]
                LeagueQueryResult(
                    it[LeagueCoreTable.leagueName],
                    configRepository.fromOverride(guild, leagueConfig),
                    it[LeagueUserTable.idx]
                )
            }
    }

    suspend fun getByGuildUserWithoutConfig(guild: Long, user: Long) = suspendTransaction(db) {
        LeagueCoreTable.innerJoin(LeagueUserTable, { leagueName }, { leagueName }).select(
            LeagueCoreTable.leagueName,
            LeagueUserTable.idx
        )
            .where { (LeagueCoreTable.guild eq guild) and (LeagueUserTable.userId eq user) and (LeagueUserTable.substitute eq false) }
            .map { it[LeagueCoreTable.leagueName] to it[LeagueUserTable.idx] }
            .toList()
    }

    private suspend fun leagueResultByGuildAndUsers(guild: Long, users: List<Long>) = suspendTransaction(db) {
        val targetSize = users.size
        if (users.size != users.distinct().size) return@suspendTransaction null
        val leagueName = LeagueCoreTable.innerJoin(LeagueUserTable, { leagueName }, { leagueName })
            .select(LeagueCoreTable.leagueName)
            .where { (LeagueCoreTable.guild eq guild) and (LeagueUserTable.userId inList users) and (LeagueUserTable.substitute eq false) }
            .groupBy(LeagueCoreTable.leagueName)
            .having { LeagueUserTable.userId.countDistinct() eq targetSize.toLong() }
            .singleOrNull()?.let { it[LeagueCoreTable.leagueName] } ?: return@suspendTransaction null
        val userToIdxMap = LeagueUserTable
            .select(LeagueUserTable.userId, LeagueUserTable.idx)
            .where {
                (LeagueUserTable.leagueName eq leagueName) and
                        (LeagueUserTable.userId inList users)
            }
            .associate { it[LeagueUserTable.userId] to it[LeagueUserTable.idx] }

        val orderedIndices = users.map { userId ->
            userToIdxMap[userId]!!
        }
        LeagueResult(leagueName, orderedIndices)
    }

    private suspend fun getLeagueResultWithoutPicks(gid: Long, uids: List<Long?>): LeagueResult? {
        val actualUids = uids.filterNotNull()
        if (uids.size != actualUids.size) return null
        val leagueResult = leagueResultByGuildAndUsers(gid, actualUids) ?: return null
        val config = configRepository.getConfig(leagueResult.leaguename)
        val isRandomBattle = config.triggers.randomBattle
        return leagueResult.takeIf { isRandomBattle }
    }

    private suspend fun getAllPicksGroupedByLeagueAndIdx(guild: Long) = suspendTransaction(db) {
        val allLeagueNames = LeagueCoreTable.select(LeagueCoreTable.leagueName).where { LeagueCoreTable.guild eq guild }
            .map { it[LeagueCoreTable.leagueName] }.toList()
        LeaguePickTable.select(LeaguePickTable.leagueName, LeaguePickTable.userIndex, LeaguePickTable.showdownId)
            .where { LeaguePickTable.leagueName inList allLeagueNames }
            .groupByMapping(
                { it[LeaguePickTable.leagueName] to it[LeaguePickTable.userIndex] },
                { it[LeaguePickTable.showdownId] }).map { (key, mons) ->
                PickedMonsData(key.first, key.second, mons.toSet())
            }
    }

    private class PickedMonsData(val leagueName: String, val idx: Int, val mons: Set<ShowdownID>)

    suspend fun leagueByShowdownReplay(
        gid: Long, game: List<SDPlayer>, ctx: BattleContext, uids: List<Long?>
    ): LeagueResult? {
        if (ctx.randomBattle) {
            return getLeagueResultWithoutPicks(gid, uids)
        }
        val matchMonIds = game.map { it.pokemon.map { mon -> mon.pokemon.toShowdownID() } }
        val allPossibleIds = pokedexRepo.getAllPossibleForms(matchMonIds.flatten())
        val allPicks = getAllPicksGroupedByLeagueAndIdx(gid)
        val (leagueResult, duration) = measureTimedValue {
            val allowedIds1 = matchMonIds[0].map { allPossibleIds[it] ?: listOf(it) }
            val allowedIds2 = matchMonIds[1].map { allPossibleIds[it] ?: listOf(it) }
            val validPm1ByLeague = mutableMapOf<String, PickedMonsData>()
            for (pick in allPicks) {
                val mons = pick.mons
                val isValid = allowedIds1.all { ids -> ids.any { it in mons } }
                if (isValid) {
                    validPm1ByLeague[pick.leagueName] = pick
                }
            }
            var resultList: List<PickedMonsData>? = null
            if (validPm1ByLeague.isNotEmpty()) {
                for (pick in allPicks) {
                    val dp1Match = validPm1ByLeague[pick.leagueName] ?: continue
                    val mons = pick.mons
                    val isValidForDp2 = allowedIds2.all { ids -> ids.any { it in mons } }
                    if (isValidForDp2) {
                        resultList = listOf(dp1Match, pick)
                        break
                    }
                }
            }
            if (resultList == null) return@measureTimedValue null
            for (i in 0..<2) {
                val pickedMons = resultList[i].mons
                for (sdMon in game[i].pokemon) {
                    val sdName = sdMon.pokemon.toShowdownID()
                    if (sdName !in pickedMons) {
                        sdMon.showdownIDOverride = allPossibleIds[sdName]?.first { it in pickedMons }
                    }
                }
            }
            LeagueResult(leaguename = resultList.first().leagueName, resultList.map { it.idx })
        }
        logger.debug { "DURATION: ${duration.inWholeMilliseconds}" }
        return leagueResult ?: getLeagueResultWithoutPicks(gid, uids)
    }
}