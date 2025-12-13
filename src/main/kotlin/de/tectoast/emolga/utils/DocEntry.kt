@file:OptIn(ExperimentalTime::class)

package de.tectoast.emolga.utils

import de.tectoast.emolga.database.Database
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.league.GamedayData
import de.tectoast.emolga.league.League
import de.tectoast.emolga.league.VideoProvideStrategy
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.json.LeagueEvent
import de.tectoast.emolga.utils.json.LeagueEvent.MatchResult
import de.tectoast.emolga.utils.json.TipGameUserData
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.records.TableSorter
import de.tectoast.emolga.utils.repeat.RepeatTask
import de.tectoast.emolga.utils.repeat.RepeatTaskType
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


class DocEntry private constructor(val league: League) {
    companion object {
        private val logger = LoggerFactory.getLogger(DocEntry::class.java)

        fun create(league: League, builder: DocEntry.() -> Unit): DocEntry {
            return DocEntry(league).apply(builder)
        }
    }

    var customDataSid: String? = null
    var spoilerDocSid: String? = null
    val statProcessors = mutableSetOf<StatProcessor>()
    var resultCreator: (suspend AdvancedResult.() -> Unit)? = null
    var sorterData: TableSorter?
        get() = sorterDatas.firstOrNull()
        set(value) {
            sorterDatas += value!!
        }
    val sorterDatas = mutableSetOf<TableSorter>()
    var setStatIfEmpty = false
    var monsOrder: (List<DraftPokemon>) -> List<String> = { l -> l.map { it.name } }
    var cancelIf: (FullGameData, Int) -> Boolean = { _: FullGameData, _: Int -> false }
    var rowNumToIndex: (Int) -> Int = { it.minus(league.newSystemGap + 1).div(league.newSystemGap) }
    private val gamedays get() = league.gamedays

    operator fun StatProcessor.unaryPlus() {
        statProcessors += this
    }

    fun newSystem(
        sorterData: TableSorter?,
        dataSheetProvider: ((memidx: Int) -> String)? = null,
        resultCreator: (suspend AdvancedResult.() -> Unit)
    ) {
        newSystem(listOfNotNull(sorterData), dataSheetProvider, resultCreator)
    }

    fun newSystem(
        sorterDatas: Collection<TableSorter>,
        dataSheetProvider: ((memidx: Int) -> String)? = null,
        resultCreator: (suspend AdvancedResult.() -> Unit)
    ) {
        fun dataSheet(memidx: Int) = dataSheetProvider?.invoke(memidx) ?: league.dataSheet
        val gap = league.newSystemGap
        +StatProcessor {
            Coord(
                sheet = dataSheet(memIdx), gdi + 3, memIdx.y(gap, monindex + 3)
            ) to DataTypeForMon.KILLS
        }
        +StatProcessor {
            Coord(
                sheet = dataSheet(memIdx), gdi + 5 + gamedays, memIdx.y(gap, monindex + 3)
            ) to DataTypeForMon.DEATHS
        }
        +StatProcessor {
            Coord(
                sheet = dataSheet(memIdx), gdi + 3, memIdx.y(gap, monindex + 3)
            ) to DataTypeForMon.WINS
        }
        +StatProcessor {
            Coord(
                sheet = dataSheet(memIdx), gdi + 5 + gamedays, memIdx.y(gap, monindex + 3)
            ) to DataTypeForMon.LOSSES
        }
        this.resultCreator = resultCreator
        this.sorterDatas += sorterDatas
    }


    suspend fun analyse(fullGameData: FullGameData, withSort: Boolean = true) {
        val config = league.config
        val store = config.replayDataStore
        if (store != null) {
            league.storeFullGameData(fullGameData)
            league.save()
            spoilerDocSid?.let { analyseWithoutCheck(fullGameData, withSort, overrideSid = it) }
            val gameday = fullGameData.gamedayData.gameday
            val currentDay = RepeatTask.getTask(league.leaguename, RepeatTaskType.RegisterInDoc)?.findGamedayOfWeek()
                ?: Int.MAX_VALUE
            if (currentDay <= gameday) return
        } else if (config.triggers.saveReplayData) {
            league.storeFullGameData(fullGameData)
            league.save()
        }
        analyseWithoutCheck(fullGameData, withSort)
    }

    suspend fun analyseWithoutCheck(
        fullGameData: FullGameData, withSort: Boolean = true, realExecute: Boolean = true, overrideSid: String? = null
    ) {
        if (fullGameData.games.isEmpty()) return
        val gamedayData = fullGameData.gamedayData
        league.config.tipgame?.let { _ ->
            league.executeTipGameLockButtonsIndividual(
                gamedayData.gameday, gamedayData.battleindex
            )
        }
        val uindices = fullGameData.uindices
        val sortedUindices = uindices.sorted()
        val matchResultJobs = mutableSetOf<Job>()
        val sid = overrideSid ?: league.sid
        val b = RequestBuilder(sid)
        val customB = customDataSid?.let(::RequestBuilder)
        val dataB = customB ?: b
        val monDataProviderCache = MonDataProviderCache(league.guild)
        val groupedData = mutableMapOf<StatProcessorRetainData, MutableMap<Coord, MonDataProvider>>()
        val dataEntriesPerUser: List<MutableList<SingleMonData>> = uindices.map { mutableListOf() }
        for ((matchNum, replayData) in fullGameData.games.withIndex()) {
            val kd = replayData.kd
            val (gameday, battleindex) = gamedayData
            if (cancelIf(fullGameData, gameday)) return
            if (gameday == -1) return
            val lookUpIndex = if (uindices[0] == sortedUindices[0]) 0 else 1
            var totalKills = 0
            var totalDeaths = 0
            kd[lookUpIndex].values.forEach {
                totalKills += it.kills
                totalDeaths += it.deaths
            }
            matchResultJobs += Database.dbScope.launch {
                ignoreDuplicatesMongo {
                    db.matchresults.insertOne(
                        MatchResult(
                            data = listOf(totalKills, totalDeaths),
                            indices = sortedUindices,
                            leaguename = league.leaguename,
                            gameday = gameday,
                            matchNum = matchNum,
                            timestamp = Clock.System.now()
                        )
                    )
                }
            }

            val picks = league.providePicksForGameday(gameday)
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
                                            fullGameData, statProcessorData, monDataProviderCache
                                        )
                                        this@buildMap[provider] = value
                                    }
                                    groupedData.getOrPut(statProcessorData.retainData) { mutableMapOf() }[coord] =
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
        for (dataEntries in dataEntriesPerUser) {
            for ((retainData, coordMap) in groupedData) {
                val monGroupedData = dataEntries.groupBy { entry ->
                    listOf(
                        if (retainData.game) entry.matchNum else -1, if (retainData.pokemon) entry.monIndex else -1
                    )
                }
                for ((_, relevantEntries) in monGroupedData) {
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
                    for ((coord, provider) in coordMap) {
                        dataB.addSingle(coord, result[provider]!!)
                    }
                }
            }
        }
        val (gameday, battleindex) = gamedayData
        val winnerIdx: Int
        val winnerIndex: Int
        val bo3 = fullGameData.games.size > 1
        if (bo3) {
            val groupBy = fullGameData.games.groupBy { it.winnerIndex }
            winnerIndex = groupBy.maxBy { it.value.size }.key
            winnerIdx = uindices[winnerIndex]
            league.config.tipgame?.let { _ ->
                TipGameUserData.updateCorrectBattles(league.leaguename, gameday, battleindex, winnerIdx)
            }
            val numbers = (0..1).map { groupBy[it]?.size ?: 0 }
            resultCreator?.let {
                AdvancedResult(
                    b = b,
                    gdi = gameday - 1,
                    index = battleindex,
                    numberOne = numbers[0],
                    numberTwo = numbers[1],
                    url = "",
                    fullGameData = fullGameData,
                    league = league,
                    winnerIdx = winnerIdx,
                    winnerIndex = winnerIndex
                ).it()
            }
        } else {
            val replayData = fullGameData.games.first()
            winnerIndex = replayData.winnerIndex
            winnerIdx = uindices[winnerIndex]
            league.config.tipgame?.let { _ ->
                TipGameUserData.updateCorrectBattles(league.leaguename, gameday, battleindex, winnerIdx)
            }
            val numbers = replayData.kd.map { kdMap ->
                kdMap.values.count { it.deaths == 0 }
            }
            resultCreator?.let {
                AdvancedResult(
                    b = b,
                    gdi = gameday - 1,
                    index = battleindex,
                    numberOne = numbers[0],
                    numberTwo = numbers[1],
                    url = replayData.url,
                    fullGameData = fullGameData,
                    league = league,
                    winnerIdx = winnerIdx,
                    winnerIndex = winnerIndex,
                ).it()
            }
        }
        customB?.execute(realExecute)
        b.withRunnable(3000) {
            if (withSort) {
                matchResultJobs.joinAll()
                sort(realExecute)
            }
        }.execute(realExecute)
    }

    suspend fun sort(realExecute: Boolean = true) {
        if (sorterDatas.isEmpty()) return
        val sid = league.sid
        val b = RequestBuilder(sid)
        logger.info("Start sorting...")
        for (tableSorter in sorterDatas) {
            val finalFormula = tableSorter.getSortedFormulas()
            b.addAll(tableSorter.formulaRangeParsed.firstHalf, finalFormula)
        }
        b.execute(realExecute)

    }
}

data class UserTableData(
    var points: Int = 0, var kills: Int = 0, var deaths: Int = 0, var wins: Int = 0, var losses: Int = 0, val index: Int
) {
    val diff get() = kills - deaths

    companion object {
        fun createFromEvents(idxs: List<Int>, events: List<LeagueEvent>): MutableMap<Int, UserTableData> {
            val data = mutableMapOf<Int, UserTableData>()
            idxs.forEachIndexed { index, l ->
                data[l] = UserTableData(index = index)
            }
            events.forEach {
                it.manipulate(data)
            }
            return data
        }
    }
}

fun interface StatProcessor {
    fun StatProcessorData.process(): Pair<Coord, MonDataProvider>
}

data class StatProcessorRetainData(var pokemon: Boolean = false, var game: Boolean = false)

class StatProcessorData {
    val memIdx: Int
    val gdi: Int
    val battleindex: Int
    val indexInBattle: Int
    val matchNum: Int
        get() = field.also { retainData.game = true }
    val monindex: Int
        get() = field.also { retainData.pokemon = true }
    val monIterationIndex: Int
        get() = field.also { retainData.pokemon = true }

    val retainData = StatProcessorRetainData()

    constructor(
        memIdx: Int,
        gdi: Int,
        battleindex: Int,
        indexInBattle: Int,
        matchNum: Int,
        monindex: Int,
        monIterationIndex: Int
    ) {
        this.memIdx = memIdx
        this.gdi = gdi
        this.battleindex = battleindex
        this.indexInBattle = indexInBattle
        this.matchNum = matchNum
        this.monindex = monindex
        this.monIterationIndex = monIterationIndex
    }
}

data class SingleMonData(
    val official: String, val matchNum: Int, val monIndex: Int, val data: Map<MonDataProvider, Any>
)

interface MonDataProvider {
    suspend fun provideData(
        fullGameData: FullGameData, statProcessorData: StatProcessorData, cache: MonDataProviderCache
    ): Any

    val accumulatePerGame: Boolean get() = false
}

data class MonDataProviderCache(val gid: Long, val officialTlCache: MutableMap<String, String> = mutableMapOf()) {
    suspend fun getTLName(official: String): String {
        return officialTlCache.getOrPut(official) {
            NameConventionsDB.convertOfficialToTL(official, gid)
                ?: error("No TL name found for $official in guild $gid") // TODO: better error handling
        }
    }
}

enum class DataTypeForMon : MonDataProvider {
    KILLS {
        override suspend fun provideData(
            fullGameData: FullGameData, statProcessorData: StatProcessorData, cache: MonDataProviderCache
        ) = fullGameData.getKD(statProcessorData).kills
    },
    DEATHS {
        override suspend fun provideData(
            fullGameData: FullGameData, statProcessorData: StatProcessorData, cache: MonDataProviderCache
        ) = fullGameData.getKD(statProcessorData).deaths
    },
    WINS {
        override suspend fun provideData(
            fullGameData: FullGameData, statProcessorData: StatProcessorData, cache: MonDataProviderCache
        ) = fullGameData.getWinsLosses(statProcessorData).first

        override val accumulatePerGame = true
    },
    LOSSES {
        override suspend fun provideData(
            fullGameData: FullGameData, statProcessorData: StatProcessorData, cache: MonDataProviderCache
        ) = fullGameData.getWinsLosses(statProcessorData).second

        override val accumulatePerGame = true
    },
    MONNAME {
        override suspend fun provideData(
            fullGameData: FullGameData, statProcessorData: StatProcessorData, cache: MonDataProviderCache
        ) = cache.getTLName(
            fullGameData.getNameKDEntry(statProcessorData).key
        )
    },
    DAMAGE_DIRECT {
        override suspend fun provideData(
            fullGameData: FullGameData, statProcessorData: StatProcessorData, cache: MonDataProviderCache
        ): Any {
            TODO("Not yet implemented")
        }
    },
    DAMAGE_INDIRECT {
        override suspend fun provideData(
            fullGameData: FullGameData, statProcessorData: StatProcessorData, cache: MonDataProviderCache
        ): Any {
            TODO("Not yet implemented")
        }
    },
    TURNS {
        override suspend fun provideData(
            fullGameData: FullGameData, statProcessorData: StatProcessorData, cache: MonDataProviderCache
        ): Any {
            TODO("Not yet implemented")
        }
    };

    fun FullGameData.getKD(data: StatProcessorData): KD {
        return getNameKDEntry(data).value
    }

    fun FullGameData.getNameKDEntry(data: StatProcessorData): Map.Entry<String, KD> =
        games[data.matchNum].kd[data.indexInBattle].entries.drop(data.monIterationIndex).first()

    fun FullGameData.getWinsLosses(data: StatProcessorData): Pair<Int, Int> {
        var wins = 0
        var looses = 0
        val game = games[data.matchNum]
        val winnerIndex = game.winnerIndex
        if (data.indexInBattle == winnerIndex) {
            wins++
        } else {
            looses++
        }
        return wins to looses

    }
}

@Serializable
data class FullGameData(
    val uindices: List<Int>,
    val gamedayData: GamedayData,
    val games: List<ReplayData>,
    val ytVideoSaveData: YTVideoSaveData = YTVideoSaveData()
) {
    suspend fun checkIfBothVideosArePresent(league: League) {
        val ytSave = ytVideoSaveData
        val shouldExecute = ytSave.vids.size == uindices.size
        val sendChannel = league.config.youtube?.sendChannel ?: return
        if (shouldExecute) {
            league.executeYoutubeSend(
                sendChannel, gamedayData.gameday, gamedayData.battleindex, VideoProvideStrategy.Subscribe(ytSave)
            )
        }
    }
}

@Serializable
data class ReplayData(
    val kd: List<Map<String, KD>>, val winnerIndex: Int, val url: String
)

@Serializable
data class KD(
    @SerialName("k") val kills: Int, @SerialName("d") val deaths: Int
)

@Serializable
data class YTVideoSaveData(
    var enabled: Boolean = false, val vids: MutableMap<Int, String> = mutableMapOf()
)

@Suppress("unused")
data class AdvancedResult(
    val b: RequestBuilder,
    val gdi: Int,
    val index: Int,
    val numberOne: Int,
    val numberTwo: Int,
    val url: String,
    val fullGameData: FullGameData,
    val league: League,
    val winnerIdx: Int,
    val winnerIndex: Int,
) {
    val firstReplayData = fullGameData.games.first()
    val idxs by lazy {
        fullGameData.uindices
    }
    val deaths by lazy {
        if (fullGameData.games.size > 1) error("deaths in AdvancedResult are only available for single replays")
        (0..1).map { firstReplayData.kd[it].values.map { s -> s.deaths == 1 } }
    }
    val kills by lazy {
        if (fullGameData.games.size > 1) error("kills in AdvancedResult are only available for single replays")
        (0..1).map {
            firstReplayData.kd[it].values.map { p -> p.kills }
        }
    }
    val higherNumber by lazy { if (numberOne > numberTwo) numberOne else numberTwo }
    val lowerNumber by lazy { if (numberOne < numberTwo) numberOne else numberTwo }
    val defaultGameplanString get() = """=HYPERLINK("$url"; "$numberOne:$numberTwo")"""
    val defaultGameplanStringWithoutUrl get() = "$numberOne:$numberTwo"
    val defaultSplitGameplanString get() = listOf("$numberOne", """=HYPERLINK("$url"; ":")""", "$numberTwo")
    val defaultSplitGameplanStringWithoutUrl get() = listOf("$numberOne", ":", "$numberTwo")
}

