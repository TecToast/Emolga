package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.increment
import de.tectoast.emolga.utils.records.UsageData
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object FullStatsDB : Table("fullstats") {
    val POKEMON = varchar("pokemon", 30)
    val KILLS = integer("kills")
    val DEATHS = integer("deaths")
    val USES = integer("uses")
    val WINS = integer("wins")
    val LOOSES = integer("looses")

    override val primaryKey = PrimaryKey(POKEMON)

    private val logger = KotlinLogging.logger {}
    fun add(pkmn: String, k: Int, d: Int, w: Boolean) = transaction {
        logger.debug("Adding to FSM {} {} {}", pkmn, k, d)
        increment(pkmn, mapOf(KILLS to k, DEATHS to d, USES to 1, WINS to if (w) 1 else 0, LOOSES to if (w) 0 else 1))
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
