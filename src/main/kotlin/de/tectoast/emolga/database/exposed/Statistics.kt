@file:OptIn(ExperimentalTime::class)

package de.tectoast.emolga.database.exposed

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.ExperimentalTime

abstract class AnalysisStatistics(type: String) : Table("st_$type") {
    val replayId = varchar("replayid", 128)
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
    val replayId = varchar("replayid", 128)
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
    val sourceMon = varchar("source", 64)
    val sourcePlayer = integer("sourceplayer")
    val targetMon = varchar("target", 64).nullable()
    val targetPlayer = integer("targetplayer").nullable()
    val move = varchar("move", 64)
}

object DamageTable : AnalysisStatistics("damage") {
    val sourceMon = varchar("source", 64)
    val sourcePlayer = integer("sourceplayer")
    val targetMon = varchar("target", 64)
    val targetPlayer = integer("targetplayer")
    val by = varchar("by", 64)
    val percent = byte("percent")
    val faint = bool("faint")
}

object HealTable : AnalysisStatistics("heal") {
    val sourceMon = varchar("source", 64)
    val sourcePlayer = integer("sourceplayer")
    val targetMon = varchar("target", 64)
    val targetPlayer = integer("targetplayer")
    val by = varchar("by", 64)
    val percent = byte("percent")
}

object SwitchTable : AnalysisStatistics("switch") {
    val type = enumeration<SwitchType>("type")
    val pokemon = varchar("pokemon", 64)
    val player = integer("player")
    val from = varchar("from", 64)

    override val primaryKey = PrimaryKey(replayId, row, type)
}

object StatusTable : AnalysisStatistics("status") {
    val sourceMon = varchar("source", 64)
    val sourcePlayer = integer("sourceplayer")
    val targetMon = varchar("target", 64)
    val targetPlayer = integer("targetplayer")
    val status = varchar("status", 16)
}

object WinTable : Table("st_wins") {
    val replayId = varchar("replayid", 128)
    val pokemon = varchar("pokemon", 64)
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

enum class SwitchType {
    IN, OUT
}

@Serializable
data class DetailedSDReplayData(
    val uploadtime: Long,
)
