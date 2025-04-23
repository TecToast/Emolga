package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.showdown.Analysis
import de.tectoast.emolga.utils.showdown.BattleContext
import de.tectoast.emolga.utils.showdown.SDPlayer
import kotlinx.datetime.Instant
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

private val logger = KotlinLogging.logger {}

abstract class AnalysisStatistics(type: String) : Table("st_$type") {
    val REPLAYID = varchar("replayid", 64)
    val ROW = integer("row")

    override val primaryKey = PrimaryKey(REPLAYID, ROW)

    companion object {

        suspend fun getCurrentAmountOfReplays() = newSuspendedTransaction {
            Start.selectAll().count()
        }

        suspend fun addDirectlyFromURL(url: String) {
            val (game, ctx) = Analysis.analyse(url)
            game.forEach { pl ->
                pl.pokemon.forEach {
                    it.draftname = Analysis.getMonName(it.pokemon, Constants.G.MY)
                }
            }
            addToStatistics(game, ctx)
        }

        suspend fun addToStatistics(game: List<SDPlayer>, ctx: BattleContext) {
            dbTransaction {
                val events = ctx.events
                val replayId = ctx.url.substringAfterLast("/").let { withPw ->
                    val split = withPw.split("-")
                    if (split.size == 2) withPw else split.dropLast(1).joinToString("-")
                }
                val start = events.start.firstOrNull()
                    ?: run { logger.warn("START not found in ${ctx.url}"); return@dbTransaction }
                Start.insertIgnore {
                    it[TIMESTAMP] = Instant.fromEpochMilliseconds(start.timestamp)
                    it[REPLAYID] = replayId
                }
                Move.batchInsert(events.move, ignore = true, shouldReturnGeneratedValues = false) {
                    this[Move.REPLAYID] = replayId
                    this[Move.ROW] = it.row
                    this[Move.SOURCE] = it.source.draftname.otherOfficial!!
                    this[Move.TARGET] = it.target?.let { m -> m.draftname.otherOfficial!! }
                    this[Move.MOVE] = it.move
                }
                Damage.batchInsert(events.damage, ignore = true, shouldReturnGeneratedValues = false) {
                    this[Damage.REPLAYID] = replayId
                    this[Damage.ROW] = it.row
                    this[Damage.SOURCE] = it.source.draftname.otherOfficial!!
                    this[Damage.TARGET] = it.target.draftname.otherOfficial!!
                    this[Damage.BY] = it.by
                    this[Damage.PERCENT] = it.percent.toByte()
                    this[Damage.FAINT] = it.faint
                }
                Heal.batchInsert(events.heal, ignore = true, shouldReturnGeneratedValues = false) {
                    this[Heal.REPLAYID] = replayId
                    this[Heal.ROW] = it.row
                    this[Heal.SOURCE] = it.source.draftname.otherOfficial!!
                    this[Heal.TARGET] = it.target.draftname.otherOfficial!!
                    this[Heal.BY] = it.by
                    this[Heal.PERCENT] = it.percent.toByte()
                }
                Switch.batchInsert(events.switch, ignore = true, shouldReturnGeneratedValues = false) {
                    this[Switch.REPLAYID] = replayId
                    this[Switch.ROW] = it.row
                    this[Switch.POKEMON] = it.pokemon.draftname.otherOfficial!!
                    this[Switch.TYPE] = it.type
                }
                Status.batchInsert(events.status, ignore = true, shouldReturnGeneratedValues = false) {
                    this[Status.REPLAYID] = replayId
                    this[Status.ROW] = it.row
                    this[Status.SOURCE] = it.source.draftname.otherOfficial!!
                    this[Status.TARGET] = it.target.draftname.otherOfficial!!
                    this[Status.STATUS] = it.status
                }
                Win.batchInsert(game.flatMap { p ->
                    val isWinner = p.winnerOfGame
                    p.pokemon.map { mon -> mon to isWinner }
                }, ignore = true, shouldReturnGeneratedValues = false) {
                    this[Win.REPLAYID] = replayId
                    this[Win.POKEMON] = it.first.draftname.otherOfficial!!
                    this[Win.WON] = it.second
                }
            }
        }
    }
}

object Start : Table("st_start") {
    val REPLAYID = varchar("replayid", 64)
    val TIMESTAMP = timestamp("timestamp")
}

object Move : AnalysisStatistics("move") {
    val SOURCE = varchar("source", 64)
    val TARGET = varchar("target", 64).nullable()
    val MOVE = varchar("move", 64)
}

object Damage : AnalysisStatistics("damage") {
    val SOURCE = varchar("source", 64)
    val TARGET = varchar("target", 64)
    val BY = varchar("by", 64)
    val PERCENT = byte("percent")
    val FAINT = bool("faint")
}

object Heal : AnalysisStatistics("heal") {
    val SOURCE = varchar("source", 64)
    val TARGET = varchar("target", 64)
    val BY = varchar("by", 64)
    val PERCENT = byte("percent")
}

object Switch : AnalysisStatistics("switch") {
    val POKEMON = varchar("pokemon", 64)
    val TYPE = enumeration<SwitchType>("type")
}

object Status : AnalysisStatistics("status") {
    val SOURCE = varchar("source", 64)
    val TARGET = varchar("target", 64)
    val STATUS = varchar("status", 16)
}

object Win : Table("st_wins") {
    val REPLAYID = varchar("replayid", 64)
    val POKEMON = varchar("pokemon", 64)
    val WON = bool("won")

    override val primaryKey = PrimaryKey(REPLAYID, POKEMON)
}

enum class SwitchType {
    IN, OUT
}