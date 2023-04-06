package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.utils.records.UsageData
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object FullStatsDB : Table("fullstats") {
    val POKEMON = varchar("pokemon", 30)
    val KILLS = integer("kills")
    val DEATHS = integer("deaths")
    val USES = integer("uses")
    val WINS = integer("wins")
    val LOOSES = integer("looses")

    private val logger = KotlinLogging.logger {}
    fun add(pkmn: String, k: Int, d: Int, w: Boolean) = transaction {
        logger.debug("Adding to FSM {} {} {}", pkmn, k, d)
        val userobj = select { POKEMON eq pkmn }.firstOrNull()
        if (userobj == null) {
            insert {
                it[POKEMON] = pkmn
                it[KILLS] = k
                it[DEATHS] = d
                it[USES] = 1
                it[WINS] = if (w) 1 else 0
                it[LOOSES] = if (w) 0 else 1
            }
        } else {
            update({ POKEMON eq POKEMON }) {
                it[KILLS] = userobj[KILLS] + k
                it[DEATHS] = userobj[DEATHS] + d
                it[USES] = userobj[USES] + 1
                it[WINS] = userobj[WINS] + if (w) 1 else 0
                it[LOOSES] = userobj[LOOSES] + if (w) 0 else 1
            }
        }
    }

    fun getData(mon: String) = transaction {
        val userobj = select { POKEMON eq mon }.firstOrNull()
        if (userobj == null) {
            UsageData(0, 0, 0, 0, 0)
        } else {
            UsageData(userobj[KILLS], userobj[DEATHS], userobj[USES], userobj[WINS], userobj[LOOSES])
        }
    }
}
