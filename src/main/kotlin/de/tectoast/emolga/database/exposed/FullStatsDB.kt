package de.tectoast.emolga.database.exposed


import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.records.UsageData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
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

    /**
     * Adds new stats to a pokemon
     * @param pokemon the pokemon to add stats to
     * @param kills the kills to add
     * @param dead true if the pokemon is dead, false otherwise
     * @param win true if the pokemon was in the winning team, false otherwise
     */
    fun add(pokemon: String, kills: Int, dead: Boolean, win: Boolean) {
        val deaths = if (dead) 1 else 0
        scope.launch {
            dbTransaction {
                logger.debug("Adding to FSM {} {} {}", pokemon, kills, deaths)
                upsert(onUpdate = {
                    it[KILLS] = KILLS.plus(kills)
                    it[DEATHS] = DEATHS.plus(deaths)
                    it[USES] = USES.plus(1)
                    it[WINS] = WINS.plus(if (win) 1 else 0)
                    it[LOOSES] = LOOSES.plus(if (!win) 1 else 0)
                }) {
                    it[POKEMON] = pokemon
                    it[KILLS] = kills
                    it[DEATHS] = deaths
                    it[USES] = 1
                    it[WINS] = if (win) 1 else 0
                    it[LOOSES] = if (!win) 1 else 0
                }
            }
        }

    }

    /**
     * Returns the stats of a pokemon
     * @param mon the pokemon to get the stats for
     * @return the stats of the pokemon
     */
    suspend fun getData(mon: String) = dbTransaction {
        val userobj = selectAll().where { POKEMON eq mon }.firstOrNull()
        if (userobj == null) {
            UsageData(0, 0, 0, 0, 0)
        } else {
            UsageData(userobj[KILLS], userobj[DEATHS], userobj[USES], userobj[WINS], userobj[LOOSES])
        }
    }
}
