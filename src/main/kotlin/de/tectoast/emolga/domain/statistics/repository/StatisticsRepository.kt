@file:OptIn(ExperimentalTime::class)

package de.tectoast.emolga.domain.statistics.repository

import de.tectoast.emolga.domain.game.service.process.analysis.AnalysisEvents
import de.tectoast.emolga.domain.game.service.process.analysis.BattleContext
import de.tectoast.emolga.domain.pokemon.model.showdownIDColumn
import de.tectoast.emolga.domain.statistics.model.DetailedSDReplayData
import de.tectoast.emolga.domain.statistics.model.SwitchType
import de.tectoast.emolga.utils.newThreadSafeCache
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.CancellationException
import mu.KotlinLogging
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.batchInsert
import org.jetbrains.exposed.v1.r2dbc.insertIgnore
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private val logger = KotlinLogging.logger {}

@Single
class StatisticsRepository(@Named("stats") private val db: R2dbcDatabase, private val httpClient: HttpClient) {

    private val lastEventsCache = newThreadSafeCache<String, AnalysisEvents>(50)


    fun getEvents(url: String): AnalysisEvents? = lastEventsCache[url]

    suspend fun getCurrentAmountOfReplays(): Long {
        return try {
            suspendTransaction(db) { StartTable.selectAll().count() }
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            logger.info { "Failed to fetch replay count from database" }
            0L
        }

    }

    suspend fun addToStatistics(ctx: BattleContext) {
        lastEventsCache[ctx.url] = ctx.events
        suspendTransaction(db) {
            val events = ctx.events
            val replayId = ctx.url.substringAfterLast("/")
            val start = events.start.firstOrNull()?.timestamp
                ?: this.run {
                    try {
                        httpClient.get(ctx.url + ".json").body<DetailedSDReplayData>().uploadtime * 1000L
                    } catch (ex: Exception) {
                        if (ex is CancellationException) throw ex
                        logger.error(ex) { "Failed to fetch replay data from ${ctx.url}" }
                        return@suspendTransaction
                    }
                }
            StartTable.insertIgnore {
                it[StartTable.timestamp] = Instant.fromEpochMilliseconds(start)
                it[StartTable.replayId] = replayId
            }
            MoveTable.batchInsert(events.move, ignore = true, shouldReturnGeneratedValues = false) {
                this[MoveTable.replayId] = replayId
                this[MoveTable.row] = it.row
                this[MoveTable.sourceMon] = it.source.showdownIDInRoster
                this[MoveTable.sourcePlayer] = it.source.player
                this[MoveTable.targetMon] = it.target?.showdownIDInRoster
                this[MoveTable.targetPlayer] = it.target?.player
                this[MoveTable.move] = it.move
            }
            DamageTable.batchInsert(events.damage, ignore = true, shouldReturnGeneratedValues = false) {
                this[DamageTable.replayId] = replayId
                this[DamageTable.row] = it.row
                this[DamageTable.sourceMon] = it.source.showdownIDInRoster
                this[DamageTable.sourcePlayer] = it.source.player
                this[DamageTable.targetMon] = it.target.showdownIDInRoster
                this[DamageTable.targetPlayer] = it.target.player
                this[DamageTable.by] = it.by
                this[DamageTable.percent] = it.percent
                this[DamageTable.faint] = it.faint
            }
            HealTable.batchInsert(events.heal, ignore = true, shouldReturnGeneratedValues = false) {
                this[HealTable.replayId] = replayId
                this[HealTable.row] = it.row
                this[HealTable.sourceMon] = it.source.showdownIDInRoster
                this[HealTable.sourcePlayer] = it.source.player
                this[HealTable.targetMon] = it.target.showdownIDInRoster
                this[HealTable.targetPlayer] = it.target.player
                this[HealTable.by] = it.by
                this[HealTable.percent] = it.percent
            }
            SwitchTable.batchInsert(events.switch, ignore = true, shouldReturnGeneratedValues = false) {
                this[SwitchTable.replayId] = replayId
                this[SwitchTable.row] = it.row
                this[SwitchTable.type] = it.type
                this[SwitchTable.pokemon] = it.pokemon.showdownIDInRoster
                this[SwitchTable.player] = it.pokemon.player
                this[SwitchTable.from] = it.from
            }
            StatusTable.batchInsert(events.status, ignore = true, shouldReturnGeneratedValues = false) {
                this[StatusTable.replayId] = replayId
                this[StatusTable.row] = it.row
                this[StatusTable.sourceMon] = it.source.showdownIDInRoster
                this[StatusTable.sourcePlayer] = it.source.player
                this[StatusTable.targetMon] = it.target.showdownIDInRoster
                this[StatusTable.targetPlayer] = it.target.player
                this[StatusTable.status] = it.status
            }
            WinTable.batchInsert(events.winLoss.flatMap { data ->
                val isWinner = data.win
                data.mons.map { mon ->
                    mon to isWinner
                }
            }, ignore = true, shouldReturnGeneratedValues = false) {
                this[WinTable.replayId] = replayId
                this[WinTable.pokemon] = it.first.showdownIDInRoster
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


abstract class AnalysisStatistics(type: String) : Table("st_$type") {
    val replayId = text("replayid")
    val row = integer("row")

    override val primaryKey = PrimaryKey(replayId, row)

    init {
        foreignKey(
            replayId to StartTable.replayId,
            onDelete = ReferenceOption.CASCADE,
            onUpdate = ReferenceOption.RESTRICT
        )
    }
}

object StartTable : Table("st_start") {
    val replayId = text("replayid")
    val timestamp = timestamp("timestamp")

    override val primaryKey = PrimaryKey(replayId)
}

object TimeTable : AnalysisStatistics("time") {
    val timestamp = timestamp("timestamp")
}

object TurnTable : AnalysisStatistics("turn") {
    val turn = integer("turn")
}

object MoveTable : AnalysisStatistics("move") {
    val sourceMon = showdownIDColumn("source")
    val sourcePlayer = integer("sourceplayer")
    val targetMon = showdownIDColumn("target").nullable()
    val targetPlayer = integer("targetplayer").nullable()
    val move = text("move")
}

object DamageTable : AnalysisStatistics("damage") {
    val sourceMon = showdownIDColumn("source")
    val sourcePlayer = integer("sourceplayer")
    val targetMon = showdownIDColumn("target")
    val targetPlayer = integer("targetplayer")
    val by = text("by")
    val percent = integer("percent")
    val faint = bool("faint")
}

object HealTable : AnalysisStatistics("heal") {
    val sourceMon = showdownIDColumn("source")
    val sourcePlayer = integer("sourceplayer")
    val targetMon = showdownIDColumn("target")
    val targetPlayer = integer("targetplayer")
    val by = text("by")
    val percent = integer("percent")
}

object SwitchTable : AnalysisStatistics("switch") {
    val type = enumeration<SwitchType>("type")
    val pokemon = showdownIDColumn("pokemon")
    val player = integer("player")
    val from = text("from")

    override val primaryKey = PrimaryKey(replayId, row, type)
}

object StatusTable : AnalysisStatistics("status") {
    val sourceMon = showdownIDColumn("source")
    val sourcePlayer = integer("sourceplayer")
    val targetMon = showdownIDColumn("target")
    val targetPlayer = integer("targetplayer")
    val status = text("status")
}

object WinTable : Table("st_wins") {
    val replayId = text("replayid")
    val pokemon = showdownIDColumn("pokemon")
    val player = integer("player")
    val won = bool("won")

    override val primaryKey = PrimaryKey(replayId, pokemon)

    init {
        foreignKey(
            replayId to StartTable.replayId,
            onDelete = ReferenceOption.CASCADE,
            onUpdate = ReferenceOption.RESTRICT
        )
    }
}