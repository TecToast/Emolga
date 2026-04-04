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

@Single
class StatisticsRepository {

    private val lastEventsCache = SizeLimitedMap<String, AnalysisEvents>(10)
    private val dbScope = createCoroutineScope("StatisticsRepository")


    fun getEvents(url: String): AnalysisEvents? = lastEventsCache[url]

    suspend fun getCurrentAmountOfReplays(): Long = dbTransaction {
        StartTable.selectAll().count()
    }

    suspend fun addDirectlyFromURL(url: String) {
        val (game, ctx, _) = Analysis.analyse(url)
        game.forEach { pl ->
            pl.pokemon.forEach {
                it.draftname = Analysis.getMonName(it.pokemon, Constants.G.MY)
            }
        }
        addToStatisticsSync(ctx)
    }

    fun addToStatisticsSync(ctx: BattleContext) {
        lastEventsCache[ctx.url] = ctx.events
        dbScope.launch {
            addToStatistics(ctx)
        }
    }

    suspend fun addToStatistics(ctx: BattleContext) {
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
            StartTable.insertIgnore {
                it[timestamp] = Instant.fromEpochMilliseconds(start)
                it[this.replayId] = replayId
            }
            MoveTable.batchInsert(events.move, ignore = true, shouldReturnGeneratedValues = false) {
                this[MoveTable.replayId] = replayId
                this[MoveTable.row] = it.row
                this[MoveTable.sourceMon] = it.source.draftname.otherOfficial!!
                this[MoveTable.sourcePlayer] = it.source.player
                this[MoveTable.targetMon] = it.target?.let { m -> m.draftname.otherOfficial!! }
                this[MoveTable.targetPlayer] = it.target?.player
                this[MoveTable.move] = it.move
            }
            DamageTable.batchInsert(events.damage, ignore = true, shouldReturnGeneratedValues = false) {
                this[DamageTable.replayId] = replayId
                this[DamageTable.row] = it.row
                this[DamageTable.sourceMon] = it.source.draftname.otherOfficial!!
                this[DamageTable.sourcePlayer] = it.source.player
                this[DamageTable.targetMon] = it.target.draftname.otherOfficial!!
                this[DamageTable.targetPlayer] = it.target.player
                this[DamageTable.by] = it.by
                this[DamageTable.percent] = it.percent.toByte()
                this[DamageTable.faint] = it.faint
            }
            HealTable.batchInsert(events.heal, ignore = true, shouldReturnGeneratedValues = false) {
                this[HealTable.replayId] = replayId
                this[HealTable.row] = it.row
                this[HealTable.sourceMon] = it.source.draftname.otherOfficial!!
                this[HealTable.sourcePlayer] = it.source.player
                this[HealTable.targetMon] = it.target.draftname.otherOfficial!!
                this[HealTable.targetPlayer] = it.target.player
                this[HealTable.by] = it.by
                this[HealTable.percent] = it.percent.toByte()
            }
            SwitchTable.batchInsert(events.switch, ignore = true, shouldReturnGeneratedValues = false) {
                this[SwitchTable.replayId] = replayId
                this[SwitchTable.row] = it.row
                this[SwitchTable.type] = it.type
                this[SwitchTable.pokemon] = it.pokemon.draftname.otherOfficial!!
                this[SwitchTable.player] = it.pokemon.player
                this[SwitchTable.from] = it.from
            }
            StatusTable.batchInsert(events.status, ignore = true, shouldReturnGeneratedValues = false) {
                this[StatusTable.replayId] = replayId
                this[StatusTable.row] = it.row
                this[StatusTable.sourceMon] = it.source.draftname.otherOfficial!!
                this[StatusTable.sourcePlayer] = it.source.player
                this[StatusTable.targetMon] = it.target.draftname.otherOfficial!!
                this[StatusTable.targetPlayer] = it.target.player
                this[StatusTable.status] = it.status
            }
            WinTable.batchInsert(events.winLoss.flatMap { data ->
                val isWinner = data.win
                data.mons.mapNotNull { mon ->
                    if (mon.draftname.otherOfficial == null) return@mapNotNull null
                    mon to isWinner
                }
            }, ignore = true, shouldReturnGeneratedValues = false) {
                this[WinTable.replayId] = replayId
                this[WinTable.pokemon] = it.first.draftname.otherOfficial!!
                this[WinTable.player] = it.first.player
                this[WinTable.won] = it.second
            }
            TimeTable.batchInsert(events.time, ignore = true, shouldReturnGeneratedValues = false) {
                this[TimeTable.replayId] = replayId
                this[TimeTable.row] = it.row
                this[TimeTable.timestamp] = Instant.fromEpochMilliseconds(it.timestamp)
            }
            TurnTable.batchInsert(events.turn, ignore = true, shouldReturnGeneratedValues = false) {
                this[TurnTable.replayId] = replayId
                this[TurnTable.row] = it.row
                this[TurnTable.turn] = it.turn
            }
        }
    }
}
