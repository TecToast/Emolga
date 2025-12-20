@file:OptIn(ExperimentalTime::class)

package de.tectoast.emolga.utils

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.exposed.AnalysisStatistics
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.database.exposed.SwitchType
import de.tectoast.emolga.league.GamedayData
import de.tectoast.emolga.league.League
import de.tectoast.emolga.league.VideoProvideStrategy
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.json.LeagueEvent
import de.tectoast.emolga.utils.json.TipGameUserData
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.records.TableSorter
import de.tectoast.emolga.utils.repeat.RepeatTask
import de.tectoast.emolga.utils.repeat.RepeatTaskType
import de.tectoast.emolga.utils.showdown.AnalysisEvents
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.into
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.util.*
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
                sheet = dataSheet(memIdx), gdi + 3, memIdx.y(gap, monIndex() + 3)
            ) to DataTypeForMon.KILLS
        }
        +StatProcessor {
            Coord(
                sheet = dataSheet(memIdx), gdi + 5 + gamedays, memIdx.y(gap, monIndex() + 3)
            ) to DataTypeForMon.DEATHS
        }
        +StatProcessor {
            Coord(
                sheet = dataSheet(memIdx), gdi + 3, memIdx.y(gap, gap)
            ) to DataTypeForMon.WINS
        }
        +StatProcessor {
            Coord(
                sheet = dataSheet(memIdx), gdi + 5 + gamedays, memIdx.y(gap, gap)
            ) to DataTypeForMon.LOSSES
        }
        this.resultCreator = resultCreator
        this.sorterDatas += sorterDatas
    }


    suspend fun analyse(fullGameData: FullGameData, withSort: Boolean = true) {
        val config = league.config
        val store = config.replayDataStore
        if (store != null || config.hideGames != null) {
            league.storeFullGameData(fullGameData)
            league.save()
            spoilerDocSid?.let { analyseWithoutCheck(fullGameData, withSort, overrideSid = it) }
            val gameday = fullGameData.gamedayData.gameday
            if (store != null) {
                val currentDay =
                    RepeatTask.getTask(league.leaguename, RepeatTaskType.RegisterInDoc)?.findGamedayOfWeek()
                        ?: Int.MAX_VALUE
                if (currentDay <= gameday) return
            } else {
                val hideGames = config.hideGames!!
                if (gameday in hideGames.gamedays) {
                    val dataForGameday = league.persistentData.replayDataStore.data[gameday]!!
                    if (dataForGameday.size == league.battleorder[gameday]!!.size) {
                        executeHideGamesDocInsertion(dataForGameday, hideGames.replayChannel, hideGames.resultChannel)
                    }
                    return
                }
            }
        } else if (config.triggers.saveReplayData) {
            league.storeFullGameData(fullGameData)
            league.save()
        }
        analyseWithoutCheck(fullGameData, withSort)
    }

    suspend fun executeHideGamesDocInsertion(
        dataForGameday: Map<Int, FullGameData>,
        replayChannel: Long,
        resultChannel: Long
    ) {
        dataForGameday.entries.sortedBy { entry -> entry.key }.forEach { (_, fullGameData) ->
            analyseWithoutCheck(fullGameData, withSort = false)
            fullGameData.sendInto(replayChannel, resultChannel, league)
        }
        delay(3000)
        sort(true)
    }

    suspend fun analyseWithoutCheck(
        fullGameData: FullGameData, withSort: Boolean = true, realExecute: Boolean = true, overrideSid: String? = null
    ) {
        if (fullGameData.games.isEmpty()) return
        if (fullGameData.games.any { game -> game.kd.size != 2 }) {
            logger.warn("Skipping analysis for league ${league.leaguename} gameday ${fullGameData.gamedayData.gameday} battle ${fullGameData.gamedayData.battleindex} due to invalid game data. (Not 1v1 games)")
            return
        }
        val gamedayData = fullGameData.gamedayData
        league.config.tipgame?.let { _ ->
            league.executeTipGameLockButtonsIndividual(
                gamedayData.gameday, gamedayData.battleindex
            )
        }
        val sid = overrideSid ?: league.sid
        val b = RequestBuilder(sid)
        val customB = customDataSid?.let(::RequestBuilder)
        val uindices = fullGameData.uindices
        val matchResultJobs = StatProcessorService.execute(
            AdditionalDataProvider(NameConventionsProviderCache(league.guild), BuiltInAnalysisProvider),
            customB ?: b,
            fullGameData,
            league.leaguename,
            league.providePicksForGameday(gamedayData.gameday),
            monsOrder,
            statProcessors
        ) {
            ignoreDuplicatesMongo {
                db.matchresults.insertOne(it)
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

@RequiresOptIn
annotation class DocEntryInternal

@OptIn(DocEntryInternal::class)
class StatProcessorData {
    val memIdx: Int
    val gdi: Int
    val battleindex: Int
    val indexInBattle: Int

    @DocEntryInternal
    val matchNum: Int

    @DocEntryInternal
    val monIndex: Int

    @DocEntryInternal
    val monIterationIndex: Int

    fun matchNum() = matchNum.also { retainData.game = true }
    fun monIndex() = monIndex.also { retainData.pokemon = true }
    fun monIterationIndex() = monIterationIndex.also { retainData.pokemon = true }

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
        this.monIndex = monindex
        this.monIterationIndex = monIterationIndex
    }
}

data class SingleMonData(
    val official: String, val matchNum: Int, val monIndex: Int, val data: Map<MonDataProvider, Any>
)

interface MonDataProvider {
    suspend fun provideData(
        fullGameData: FullGameData, statProcessorData: StatProcessorData, provider: AdditionalDataProvider
    ): Any

    val accumulatePerGame: Boolean get() = false
}

data class AdditionalDataProvider(
    val monDataProviderCache: MonDataProviderCache,
    val analysisProvider: AnalysisProvider,
)

interface MonDataProviderCache {
    suspend fun getTLName(official: String): String
    suspend fun getOfficialEnglishName(official: String): String
}

interface AnalysisProvider {
    fun getEvents(replayData: ReplayData): AnalysisEvents
}

object BuiltInAnalysisProvider : AnalysisProvider {
    override fun getEvents(replayData: ReplayData): AnalysisEvents {
        return AnalysisStatistics.lastEventsCache[replayData.url]
            ?: error("No analysis events found for replay ${replayData.url}") // TODO: better error handling
    }
}

data class NameConventionsProviderCache(
    val gid: Long,
    val officialTlCache: MutableMap<String, String> = mutableMapOf(),
    val officialEnCache: MutableMap<String, String> = mutableMapOf(),
) : MonDataProviderCache {
    override suspend fun getTLName(official: String): String {
        return officialTlCache.getOrPut(official) {
            NameConventionsDB.convertOfficialToTL(official, gid)
                ?: error("No TL name found for $official in guild $gid") // TODO: better error handling
        }
    }

    override suspend fun getOfficialEnglishName(official: String): String {
        return officialEnCache.getOrPut(official) {
            NameConventionsDB.getSDTranslation(official, gid, english = true)?.official
                ?: error("No English name found for $official in guild $gid") // TODO: better error handling
        }
    }
}

@OptIn(DocEntryInternal::class)
enum class DataTypeForMon : MonDataProvider {
    KILLS {
        override suspend fun provideData(
            fullGameData: FullGameData, statProcessorData: StatProcessorData, provider: AdditionalDataProvider
        ) = fullGameData.getKD(statProcessorData).kills
    },
    DEATHS {
        override suspend fun provideData(
            fullGameData: FullGameData, statProcessorData: StatProcessorData, provider: AdditionalDataProvider
        ) = fullGameData.getKD(statProcessorData).deaths
    },
    WINS {
        override suspend fun provideData(
            fullGameData: FullGameData, statProcessorData: StatProcessorData, provider: AdditionalDataProvider
        ) = fullGameData.getWinsLosses(statProcessorData).first

        override val accumulatePerGame = true
    },
    LOSSES {
        override suspend fun provideData(
            fullGameData: FullGameData, statProcessorData: StatProcessorData, provider: AdditionalDataProvider
        ) = fullGameData.getWinsLosses(statProcessorData).second

        override val accumulatePerGame = true
    },
    MONNAME {
        override suspend fun provideData(
            fullGameData: FullGameData, statProcessorData: StatProcessorData, provider: AdditionalDataProvider
        ) = provider.monDataProviderCache.getTLName(
            fullGameData.getNameKDEntry(statProcessorData).key
        )
    },
    DAMAGE_DIRECT {
        override suspend fun provideData(
            fullGameData: FullGameData, statProcessorData: StatProcessorData, provider: AdditionalDataProvider
        ) = fullGameData.getDamageDealt(statProcessorData, provider, active = true)
    },
    DAMAGE_INDIRECT {
        override suspend fun provideData(
            fullGameData: FullGameData, statProcessorData: StatProcessorData, provider: AdditionalDataProvider
        ) = fullGameData.getDamageDealt(statProcessorData, provider, active = false)
    },
    DAMAGE_TAKEN {
        override suspend fun provideData(
            fullGameData: FullGameData, statProcessorData: StatProcessorData, provider: AdditionalDataProvider
        ) = fullGameData.getDamageTaken(statProcessorData, provider)
    },
    TURNS {
        override suspend fun provideData(
            fullGameData: FullGameData, statProcessorData: StatProcessorData, provider: AdditionalDataProvider
        ) = fullGameData.getActiveTurns(statProcessorData, provider)
    };

    private data class StatisticSource(val events: AnalysisEvents, val englishOfficial: String, val indexInData: Int)

    private suspend fun FullGameData.getStatisticSource(
        data: StatProcessorData, provider: AdditionalDataProvider
    ): StatisticSource {
        val replayData = games[data.matchNum]
        val events = provider.analysisProvider.getEvents(replayData)
        val winnerInSDBattle = events.winLoss.first { it.win }.indexInBattle
        val winnerInReplayData = replayData.winnerIndex
        val indexInData = if (winnerInSDBattle == winnerInReplayData) {
            data.indexInBattle
        } else {
            1 - data.indexInBattle
        }
        val englishOfficial = provider.monDataProviderCache.getOfficialEnglishName(getNameKDEntry(data).key)
        return StatisticSource(events, englishOfficial, indexInData)
    }

    suspend fun FullGameData.getActiveTurns(data: StatProcessorData, provider: AdditionalDataProvider): Int {
        val (events, englishOfficial, indexInData) = getStatisticSource(data, provider)
        val allSwitchOutRowsOfPlayer =
            events.switch.filter { it.pokemon.player == indexInData && it.type == SwitchType.OUT }
                .mapTo(mutableSetOf()) { it.row }
        val allSwitches =
            events.switch.filter { it.pokemon.player == indexInData && it.pokemon.draftname.otherOfficial == englishOfficial }
        val (switchIns, switchOuts) = allSwitches.partition { it.type == SwitchType.IN }
        val faint = events.damage.firstOrNull {
            it.target.player == indexInData && it.target.draftname.otherOfficial == englishOfficial && it.faint
        }?.row ?: Int.MAX_VALUE
        val turns = events.turn.associateTo(TreeMap()) { it.row to it.turn }
        var turnCount = 0
        for (switch in switchIns) {
            var startTurn = turns.floorEntry(switch.row)?.value ?: 0
            if (switch.row !in allSwitchOutRowsOfPlayer) {
                startTurn++
            }
            val matchingSwitchOut = switchOuts.firstOrNull { it.row > switch.row }
            val endRow = matchingSwitchOut?.row ?: faint
            var endTurn = turns.floorEntry(endRow)?.value ?: 1
            if (matchingSwitchOut?.from != "Switch") {
                endTurn++
            }
            turnCount += (endTurn - startTurn).coerceAtLeast(0)
        }
        return turnCount
    }

    suspend fun FullGameData.getDamageDealt(
        data: StatProcessorData,
        provider: AdditionalDataProvider,
        active: Boolean
    ): Int {
        val (events, englishOfficial, indexInData) = getStatisticSource(data, provider)
        return events.damage.filter { it.source.player == indexInData && it.target.player != indexInData && it.source.draftname.otherOfficial == englishOfficial && it.active == active }
            .sumOf { it.percent }
    }

    suspend fun FullGameData.getDamageTaken(data: StatProcessorData, provider: AdditionalDataProvider): Int {
        val (events, englishOfficial, indexInData) = getStatisticSource(data, provider)
        return events.damage.filter { it.target.player == indexInData && it.target.draftname.otherOfficial == englishOfficial }
            .sumOf { it.percent }
    }

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

    suspend fun sendInto(replayChannel: Long, resultChannel: Long, league: League) {
        val replay by lazy { jda.getTextChannelById(replayChannel)!! }
        val result = jda.getTextChannelById(resultChannel)!!
        val gameday = gamedayData.gameday
        val fullGameData = this
        for (game in games) {
            if (game.url.startsWith("https")) {
                val tosend = MessageCreate(
                    content = game.url,
                    embeds = league.appendedEmbed(null, uindices, gameday).build().into()
                )
                replay.sendMessage(tosend).queue()
            }
            with(league) {
                result.sendResultEntryMessage(gameday, ResultEntryDescription.Bo3(fullGameData))
            }
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

