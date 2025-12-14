package de.tectoast.emolga.utils

import de.tectoast.emolga.database.Database
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.json.LeagueEvent
import de.tectoast.emolga.utils.records.Coord
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mu.KotlinLogging
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private val logger = KotlinLogging.logger {}

object StatProcessorService {
    @OptIn(ExperimentalTime::class)
    suspend fun execute(
        additionalDataProvider: AdditionalDataProvider,
        b: SimpleRequestBuilder,
        fullGameData: FullGameData,
        leaguename: String,
        picks: Map<Int, List<DraftPokemon>>,
        monsOrder: (List<DraftPokemon>) -> List<String>,
        statProcessors: Collection<StatProcessor>,
        matchResultHandler: suspend (LeagueEvent.MatchResult) -> Unit
    ): Set<Job> {
        val gamedayData = fullGameData.gamedayData
        val uindices = fullGameData.uindices
        val matchResultJobs = mutableSetOf<Job>()
        val sortedUindices = uindices.sorted()
        val groupedData: List<MutableMap<GroupNumbers, MutableMap<Coord, MonDataProvider>>> =
            uindices.map { mutableMapOf() }
        val dataEntriesPerUser: List<MutableList<SingleMonData>> = uindices.map { mutableListOf() }
        for ((matchNum, replayData) in fullGameData.games.withIndex()) {
            val kd = replayData.kd
            val (gameday, battleindex) = gamedayData
            if (gameday == -1) return emptySet()
            val lookUpIndex = if (uindices[0] == sortedUindices[0]) 0 else 1
            var totalKills = 0
            var totalDeaths = 0
            kd[lookUpIndex].values.forEach {
                totalKills += it.kills
                totalDeaths += it.deaths
            }
            matchResultJobs += Database.dbScope.launch {
                matchResultHandler(
                    LeagueEvent.MatchResult(
                        data = listOf(totalKills, totalDeaths),
                        indices = sortedUindices,
                        leaguename = leaguename,
                        gameday = gameday,
                        matchNum = matchNum,
                        timestamp = Clock.System.now()
                    )
                )
            }

            for ((i, idx) in uindices.withIndex()) {
                if (idx in picks) {
                    val monsInOrder = monsOrder(picks[idx]!!)
                    for ((iterationIndex, tuple) in kd[i].entries.withIndex()) {
                        val (mon, _) = tuple
                        val monIndex = monsInOrder.indexOfFirst {
                            it == mon
                        }
                        if (monIndex == -1) {
                            logger.warn("Mon $mon not found in picks of $idx")
                            continue
                        }
                        val monData = buildMap {
                            for (processor in statProcessors) {
                                val statProcessorData = StatProcessorData(
                                    memIdx = idx,
                                    gdi = gameday - 1,
                                    battleindex = battleindex,
                                    indexInBattle = i,
                                    matchNum = matchNum,
                                    monindex = monIndex,
                                    monIterationIndex = iterationIndex
                                )
                                with(processor) {
                                    val (coord, provider) = statProcessorData.process()
                                    if (provider !in this@buildMap) {
                                        val value = provider.provideData(
                                            fullGameData, statProcessorData, additionalDataProvider
                                        )
                                        this@buildMap[provider] = value
                                    }
                                    groupedData[i].getOrPut(
                                        GroupNumbers.fromRetainData(
                                            statProcessorData.retainData,
                                            matchNum,
                                            monIndex
                                        )
                                    ) { mutableMapOf() }[coord] =
                                        provider
                                }
                            }
                        }
                        dataEntriesPerUser[i] += SingleMonData(
                            official = mon, matchNum = matchNum, monIndex = monIndex, data = monData
                        )
                    }
                }
            }
        }
        for ((i, dataEntries) in dataEntriesPerUser.withIndex()) {
            for ((groupNumbers, coordMap) in groupedData[i]) {
                val relevantEntries = dataEntries.filter {
                    groupNumbers.matchesMatchNum(it.matchNum) && groupNumbers.matchesMonIndex(it.monIndex)
                }
                for ((coord, provider) in coordMap) {
                    val result = if (relevantEntries.size > 1) {
                        val accumulator = mutableMapOf<MonDataProvider, Int>()
                        for (provider in coordMap.values.toSet()) {
                            if (provider.accumulatePerGame) {
                                accumulator[provider] =
                                    relevantEntries.groupBy { it.matchNum }.values.sumOf { entriesPerGame -> entriesPerGame.first().data[provider] as Int }
                            } else {
                                accumulator[provider] = relevantEntries.sumOf { it.data[provider] as Int }
                            }
                        }
                        accumulator
                    } else relevantEntries.first().data
                    b.addSingle(coord, result[provider]!!)
                }
            }
        }
        return matchResultJobs
    }
}

private data class GroupNumbers(val matchNum: Int, val monIndex: Int) {
    fun matchesMatchNum(other: Int) = matchNum == -1 || matchNum == other
    fun matchesMonIndex(other: Int) = monIndex == -1 || monIndex == other

    companion object {
        fun fromRetainData(retainData: StatProcessorRetainData, matchNum: Int, monIndex: Int): GroupNumbers {
            return GroupNumbers(
                matchNum = if (retainData.game) matchNum else -1,
                monIndex = if (retainData.pokemon) monIndex else -1
            )
        }
    }
}