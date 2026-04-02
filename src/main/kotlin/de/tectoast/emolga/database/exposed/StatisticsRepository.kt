@file:OptIn(ExperimentalTime::class)

package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.SizeLimitedMap
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.httpClient
import de.tectoast.emolga.utils.showdown.Analysis
import de.tectoast.emolga.utils.showdown.AnalysisEvents
import de.tectoast.emolga.utils.showdown.BattleContext
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.jetbrains.exposed.v1.r2dbc.batchInsert
import org.jetbrains.exposed.v1.r2dbc.insertIgnore
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.koin.core.annotation.Single
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private val logger = KotlinLogging.logger {}

interface StatisticsRepository {
    fun getEvents(url: String): AnalysisEvents?
    suspend fun getCurrentAmountOfReplays(): Long
    suspend fun addDirectlyFromURL(url: String)
    fun addToStatisticsSync(ctx: BattleContext)
    suspend fun addToStatistics(ctx: BattleContext)
}

@Single
class PostgresStatisticsRepository(
    private val startTable: StartTable,
    private val moveTable: MoveTable,
    private val damageTable: DamageTable,
    private val healTable: HealTable,
    private val switchTable: SwitchTable,
    private val statusTable: StatusTable,
    private val winTable: WinTable,
    private val timeTable: TimeTable,
    private val turnTable: TurnTable,
) : StatisticsRepository {

    private val lastEventsCache = SizeLimitedMap<String, AnalysisEvents>(10)
    private val dbScope = createCoroutineScope("StatisticsRepository")


    override fun getEvents(url: String): AnalysisEvents? = lastEventsCache[url]

    override suspend fun getCurrentAmountOfReplays(): Long = dbTransaction {
        startTable.selectAll().count()
    }

    override suspend fun addDirectlyFromURL(url: String) {
        val (game, ctx, _) = Analysis.analyse(url)
        game.forEach { pl ->
            pl.pokemon.forEach {
                it.draftname = Analysis.getMonName(it.pokemon, Constants.G.MY)
            }
        }
        addToStatisticsSync(ctx)
    }

    override fun addToStatisticsSync(ctx: BattleContext) {
        lastEventsCache[ctx.url] = ctx.events
        dbScope.launch {
            addToStatistics(ctx)
        }
    }

    override suspend fun addToStatistics(ctx: BattleContext) {
        dbTransaction {
            val events = ctx.events
            val replayId = ctx.url.substringAfterLast("/")
            val start = events.start.firstOrNull()?.timestamp
                ?: run {
                    try {
                        httpClient.get(ctx.url + ".json").body<DetailedSDReplayData>().uploadtime * 1000L
                    } catch (ex: Exception) {
                        if (ex is CancellationException) throw ex
                        logger.error(ex) { "Failed to fetch replay data from ${ctx.url}" }
                        return@dbTransaction
                    }
                }
            startTable.insertIgnore {
                it[TIMESTAMP] = Instant.fromEpochMilliseconds(start)
                it[REPLAYID] = replayId
            }
            moveTable.batchInsert(events.move, ignore = true, shouldReturnGeneratedValues = false) {
                this[moveTable.REPLAYID] = replayId
                this[moveTable.ROW] = it.row
                this[moveTable.SOURCE] = it.source.draftname.otherOfficial!!
                this[moveTable.SOURCEPLAYER] = it.source.player
                this[moveTable.TARGET] = it.target?.let { m -> m.draftname.otherOfficial!! }
                this[moveTable.TARGETPLAYER] = it.target?.player
                this[moveTable.MOVE] = it.move
            }
            damageTable.batchInsert(events.damage, ignore = true, shouldReturnGeneratedValues = false) {
                this[damageTable.REPLAYID] = replayId
                this[damageTable.ROW] = it.row
                this[damageTable.SOURCE] = it.source.draftname.otherOfficial!!
                this[damageTable.SOURCEPLAYER] = it.source.player
                this[damageTable.TARGET] = it.target.draftname.otherOfficial!!
                this[damageTable.TARGETPLAYER] = it.target.player
                this[damageTable.BY] = it.by
                this[damageTable.PERCENT] = it.percent.toByte()
                this[damageTable.FAINT] = it.faint
            }
            healTable.batchInsert(events.heal, ignore = true, shouldReturnGeneratedValues = false) {
                this[healTable.REPLAYID] = replayId
                this[healTable.ROW] = it.row
                this[healTable.SOURCE] = it.source.draftname.otherOfficial!!
                this[healTable.SOURCEPLAYER] = it.source.player
                this[healTable.TARGET] = it.target.draftname.otherOfficial!!
                this[healTable.TARGETPLAYER] = it.target.player
                this[healTable.BY] = it.by
                this[healTable.PERCENT] = it.percent.toByte()
            }
            switchTable.batchInsert(events.switch, ignore = true, shouldReturnGeneratedValues = false) {
                this[switchTable.REPLAYID] = replayId
                this[switchTable.ROW] = it.row
                this[switchTable.TYPE] = it.type
                this[switchTable.POKEMON] = it.pokemon.draftname.otherOfficial!!
                this[switchTable.PLAYER] = it.pokemon.player
                this[switchTable.FROM] = it.from
            }
            statusTable.batchInsert(events.status, ignore = true, shouldReturnGeneratedValues = false) {
                this[statusTable.REPLAYID] = replayId
                this[statusTable.ROW] = it.row
                this[statusTable.SOURCE] = it.source.draftname.otherOfficial!!
                this[statusTable.SOURCEPLAYER] = it.source.player
                this[statusTable.TARGET] = it.target.draftname.otherOfficial!!
                this[statusTable.TARGETPLAYER] = it.target.player
                this[statusTable.STATUS] = it.status
            }
            winTable.batchInsert(events.winLoss.flatMap { data ->
                val isWinner = data.win
                data.mons.mapNotNull { mon ->
                    if (mon.draftname.otherOfficial == null) return@mapNotNull null
                    mon to isWinner
                }
            }, ignore = true, shouldReturnGeneratedValues = false) {
                this[winTable.REPLAYID] = replayId
                this[winTable.POKEMON] = it.first.draftname.otherOfficial!!
                this[winTable.PLAYER] = it.first.player
                this[winTable.WON] = it.second
            }
            timeTable.batchInsert(events.time, ignore = true, shouldReturnGeneratedValues = false) {
                this[timeTable.REPLAYID] = replayId
                this[timeTable.ROW] = it.row
                this[timeTable.TIMESTAMP] = Instant.fromEpochMilliseconds(it.timestamp)
            }
            turnTable.batchInsert(events.turn, ignore = true, shouldReturnGeneratedValues = false) {
                this[turnTable.REPLAYID] = replayId
                this[turnTable.ROW] = it.row
                this[turnTable.TURN] = it.turn
            }
        }
    }
}
