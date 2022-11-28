package de.tectoast.emolga.utils.sql.managers

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.utils.records.UsageData
import de.tectoast.emolga.utils.sql.base.DataManager
import de.tectoast.emolga.utils.sql.base.DataManager.ResultsFunction
import de.tectoast.emolga.utils.sql.base.columns.IntColumn
import de.tectoast.emolga.utils.sql.base.columns.StringColumn
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

object FullStatsManager : DataManager("fullstats") {
    private val POKEMON = StringColumn("pokemon", this)
    private val KILLS = IntColumn("kills", this)
    private val DEATHS = IntColumn("deaths", this)
    private val USES = IntColumn("uses", this)
    private val WINS = IntColumn("wins", this)
    private val LOOSES = IntColumn("looses", this)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, t ->
        logger.error("ERROR IN FULLSTATS SCOPE", t)
        Command.sendToMe("Error in fullstats scope, look in console")
    })

    init {
        setColumns(POKEMON, KILLS, DEATHS, USES, WINS, LOOSES)
    }

    fun add(pokemon: String, kills: Int, deaths: Int, win: Boolean) {
        logger.debug("Adding to FSM {} {} {}", pokemon, kills, deaths)
        scope.launch { addStatistics(pokemon, kills, deaths, 1, if (win) 1 else 0, if (win) 0 else 1) }
    }

    fun getData(mon: String?): UsageData {
        return read(selectAll(POKEMON.check(mon)), ResultsFunction { s ->
            mapFirst(
                s, {
                    UsageData(
                        KILLS.getValue(
                            it
                        ), DEATHS.getValue(it), USES.getValue(it), WINS.getValue(it), LOOSES.getValue(it)
                    )
                },
                UsageData(0, 0, 0, 0, 0)
            )
        })
    }

    private val logger = LoggerFactory.getLogger(FullStatsManager::class.java)

}
