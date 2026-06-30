package de.tectoast.emolga.domain.league.doc.service

import de.tectoast.emolga.domain.league.doc.model.*
import de.tectoast.emolga.domain.league.doc.service.provider.data.DocDataProviderDispatcher
import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.domain.league.gamedata.model.FullGameData
import de.tectoast.emolga.domain.league.gamedata.model.LeagueEvent
import de.tectoast.emolga.domain.league.gamedata.model.LeagueEventSpecificData
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.utils.dsl.Coord
import de.tectoast.emolga.utils.sheetupdate.SheetUpdateContext
import mu.KotlinLogging
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private val logger = KotlinLogging.logger {}

@Single
class StatProcessorService(private val clock: Clock, private val docProviderDispatcher: DocDataProviderDispatcher) {
    @OptIn(ExperimentalTime::class)
    suspend fun execute(
        additionalDataProvider: AdditionalDataProvider,
        sheet: SheetUpdateContext,
        fullGameData: FullGameData,
        leaguename: String,
        picks: Map<Int, List<DraftPokemon>>,
        monsOrder: suspend (List<DraftPokemon>) -> List<ShowdownID>,
        statProcessors: Collection<StatProcessor>,
    ): List<LeagueEvent> {
        val week = fullGameData.week
        val battleIndex = fullGameData.battleIndex
        val uindices = fullGameData.uindices
        val matchResults = mutableListOf<LeagueEvent>()
        val sortedUindices = uindices.sorted()
        val groupedData: List<MutableMap<GroupNumbers, MutableMap<Coord, DocDataProviderConfig>>> =
            uindices.map { mutableMapOf() }
        val dataEntriesPerUser: List<MutableList<SingleMonData>> = uindices.map { mutableListOf() }
        for ((matchNum, replayData) in fullGameData.games.withIndex()) {
            val kd = replayData.kd
            if (week == -1) return emptyList()
            val lookUpIndex = if (uindices[0] == sortedUindices[0]) 0 else 1
            var totalKills = 0
            var totalDeaths = 0
            kd[lookUpIndex].forEach {
                totalKills += it.kills
                totalDeaths += it.deaths
            }
            matchResults += LeagueEvent(
                week = week,
                matchNum = matchNum,
                timestamp = clock.now(),
                leagueName = leaguename,
                uindices = sortedUindices,
                specificData = LeagueEventSpecificData.MatchResult(listOf(totalKills, totalDeaths)),
            )

            for ((i, idx) in uindices.withIndex()) {
                if (idx in picks) {
                    val monsInOrder = monsOrder(picks[idx]!!)
                    for ((iterationIndex, kdWithName) in kd[i].withIndex()) {
                        val mon = kdWithName.name
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
                                    weekIndex = week - 1,
                                    battleindex = battleIndex,
                                    indexInBattle = i,
                                    matchNum = matchNum,
                                    monindex = monIndex,
                                    monIterationIndex = iterationIndex
                                )
                                val coord = processor.coord.eval(statProcessorData)
                                val provider = processor.provider
                                if (provider !in this@buildMap) {
                                    val value = docProviderDispatcher.provideData(
                                        provider, fullGameData, statProcessorData, additionalDataProvider
                                    )
                                    this@buildMap[provider] = value
                                }
                                groupedData[i].getOrPut(
                                    GroupNumbers.fromRetainData(
                                        statProcessorData.retainData, matchNum, monIndex
                                    )
                                ) { mutableMapOf() }[coord] = provider
                            }
                        }
                        dataEntriesPerUser[i] += SingleMonData(
                            showdownId = mon, matchNum = matchNum, monIndex = monIndex, data = monData
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
                    val accumulationMode = provider.accumulationMode
                    val result =
                        if (relevantEntries.size == 1 || accumulationMode == AccumulationMode.NEVER) relevantEntries.first().data[provider]!! else when (accumulationMode) {
                            AccumulationMode.DEFAULT -> relevantEntries.sumOf { it.data[provider] as Int }
                            AccumulationMode.PER_GAME -> relevantEntries.groupBy { it.matchNum }.values.sumOf { entriesPerGame -> entriesPerGame.first().data[provider] as Int }
                        }
                    sheet.addSingle(coord, result)
                }
            }
        }
        return matchResults
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
}
