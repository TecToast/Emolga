package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.toSDName
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object Crinchy : IdTable<String>("crinchy") {
    override val id = varchar("id", 50).entityId()
    override val primaryKey = PrimaryKey(id)

    private val turns = integer("turns")
    private val movesThatCouldHit = integer("movesthathit")
    private val gamemode = enumeration<SDFormat>("gamemode")

    suspend fun insertReplay(replayId: String, lines: List<String>) {
        newSuspendedTransaction {
            insertIgnore { tr ->
                tr[id] = replayId
                tr[turns] = lines.lastOrNull { it.startsWith("|turn|") }?.substring(6)?.toIntOrNull() ?: 0
                tr[movesThatCouldHit] = run {
                    var count = 0
                    for ((index, line) in lines.withIndex()) {
                        if (line.startsWith("|move|") && Command.movesJSON[line.split("|")[3].toSDName()]?.jsonObject?.get(
                                "category"
                            )?.jsonPrimitive?.content != "Status" && lines.getOrNull(index + 1)?.let {
                                (it.startsWith("|-activate") && it.split("|")[3].contains("move: "))
                                        || it.startsWith("|-fail")
                                        || it.startsWith("|-immune")
                            } == false
                        ) ++count
                    }
                    count
                }
                tr[gamemode] = lines.first { it.startsWith("|tier|") }.substring(6).let {
                    if ("VGC" in it || "Doubles" in it) SDFormat.Doubles else SDFormat.Singles
                }
            }
        }
    }

    suspend fun allStats(): String {
        return newSuspendedTransaction {
            val map = mutableMapOf<SDFormat, CrinchyStats>()
            selectAll().fold(map) { stats, row ->
                stats.apply {
                    stats.getOrPut(row[gamemode]) { CrinchyStats() }.apply {
                        replayCount++
                        turnCount += row[turns]
                        moveHitCount += row[movesThatCouldHit]
                    }
                }
            }.let { stats ->
                val all = stats.values
                """
Gesamt-Replay-Anzahl: ${all.sumOf { it.replayCount }}
Gesamt-Turn-Anzahl: ${all.sumOf { it.turnCount }}
Gesamt-Moves-That-Could-Hit-Anzahl: ${all.sumOf { it.moveHitCount }}
""".trimIndent()
            }
        }
    }

    data class CrinchyStats(
        var replayCount: Int = 0, var turnCount: Int = 0, var moveHitCount: Int = 0
    )
}

enum class SDFormat {
    Singles, Doubles;
}

