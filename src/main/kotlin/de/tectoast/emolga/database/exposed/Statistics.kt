@file:OptIn(ExperimentalTime::class)

package de.tectoast.emolga.database.exposed

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp
import org.koin.core.annotation.Single
import kotlin.time.ExperimentalTime

abstract class AnalysisStatistics(type: String, startTable: StartTable) : Table("st_$type") {
    val REPLAYID = varchar("replayid", 128)
    val ROW = integer("row")

    override val primaryKey = PrimaryKey(REPLAYID, ROW)

    init {
        foreignKey(
            REPLAYID to startTable.REPLAYID,
            onDelete = ReferenceOption.CASCADE,
            onUpdate = ReferenceOption.RESTRICT
        )
    }
}

@Single
class StartTable : Table("st_start") {
    val REPLAYID = varchar("replayid", 128)
    val TIMESTAMP = timestamp("timestamp")

    override val primaryKey = PrimaryKey(REPLAYID)
}

@Single
class TimeTable(startTable: StartTable) : AnalysisStatistics("time", startTable) {
    val TIMESTAMP = timestamp("timestamp")
}

@Single
class TurnTable(startTable: StartTable) : AnalysisStatistics("turn", startTable) {
    val TURN = integer("turn")
}

@Single
class MoveTable(startTable: StartTable) : AnalysisStatistics("move", startTable) {
    val SOURCE = varchar("source", 64)
    val SOURCEPLAYER = integer("sourceplayer")
    val TARGET = varchar("target", 64).nullable()
    val TARGETPLAYER = integer("targetplayer").nullable()
    val MOVE = varchar("move", 64)
}

@Single
class DamageTable(startTable: StartTable) : AnalysisStatistics("damage", startTable) {
    val SOURCE = varchar("source", 64)
    val SOURCEPLAYER = integer("sourceplayer")
    val TARGET = varchar("target", 64)
    val TARGETPLAYER = integer("targetplayer")
    val BY = varchar("by", 64)
    val PERCENT = byte("percent")
    val FAINT = bool("faint")
}

@Single
class HealTable(startTable: StartTable) : AnalysisStatistics("heal", startTable) {
    val SOURCE = varchar("source", 64)
    val SOURCEPLAYER = integer("sourceplayer")
    val TARGET = varchar("target", 64)
    val TARGETPLAYER = integer("targetplayer")
    val BY = varchar("by", 64)
    val PERCENT = byte("percent")
}

@Single
class SwitchTable(startTable: StartTable) : AnalysisStatistics("switch", startTable) {
    val TYPE = enumeration<SwitchType>("type")
    val POKEMON = varchar("pokemon", 64)
    val PLAYER = integer("player")
    val FROM = varchar("from", 64)

    override val primaryKey = PrimaryKey(REPLAYID, ROW, TYPE)
}

@Single
class StatusTable(startTable: StartTable) : AnalysisStatistics("status", startTable) {
    val SOURCE = varchar("source", 64)
    val SOURCEPLAYER = integer("sourceplayer")
    val TARGET = varchar("target", 64)
    val TARGETPLAYER = integer("targetplayer")
    val STATUS = varchar("status", 16)
}

@Single
class WinTable(startTable: StartTable) : Table("st_wins") {
    val REPLAYID = varchar("replayid", 128)
    val POKEMON = varchar("pokemon", 64)
    val PLAYER = integer("player")
    val WON = bool("won")

    override val primaryKey = PrimaryKey(REPLAYID, POKEMON)

    init {
        foreignKey(
            REPLAYID to startTable.REPLAYID,
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
