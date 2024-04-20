package de.tectoast.emolga.database.exposed


import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.records.UsageData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.upsert

object FullStatsDB : Table("fullstats") {
    val POKEMON = varchar("pokemon", 30)
    val KILLS = integer("kills")
    val DEATHS = integer("deaths")
    val USES = integer("uses")
    val WINS = integer("wins")
    val LOOSES = integer("looses")

    override val primaryKey = PrimaryKey(POKEMON)

    private val logger = KotlinLogging.logger {}
    private val scope = createCoroutineScope("FullStatsDB", Dispatchers.IO)
    fun add(pkmn: String, k: Int, d: Int, w: Boolean) {
        scope.launch {
            newSuspendedTransaction {
                logger.debug("Adding to FSM {} {} {}", pkmn, k, d)
                upsert(
                    onUpdate = listOf(
                        KILLS to KILLS.plus(k),
                        DEATHS to DEATHS.plus(d),
                        USES to USES.plus(1),
                        WINS to WINS.plus(if (w) 1 else 0),
                        LOOSES to LOOSES.plus(if (!w) 1 else 0)
                    )
                ) {
                    it[POKEMON] = pkmn
                    it[KILLS] = k
                    it[DEATHS] = d
                    it[USES] = 1
                    it[WINS] = if (w) 1 else 0
                    it[LOOSES] = if (!w) 1 else 0
                }
            }
        }

    }

    suspend fun getData(mon: String) = newSuspendedTransaction {
        val userobj = selectAll().where { POKEMON eq mon }.firstOrNull()
        if (userobj == null) {
            UsageData(0, 0, 0, 0, 0)
        } else {
            UsageData(userobj[KILLS], userobj[DEATHS], userobj[USES], userobj[WINS], userobj[LOOSES])
        }
    }
}
