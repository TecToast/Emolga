package de.tectoast.emolga.utils

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.Command.Companion.loadSD
import de.tectoast.jsolf.JSONObject
import dev.minn.jda.ktx.util.SLF4J
import kotlinx.coroutines.*
import org.slf4j.Logger

class ModManager(name: String, datapath: String) {
    lateinit var dex: JSONObject
    lateinit var learnsets: JSONObject
    lateinit var moves: JSONObject
    lateinit var typechart: JSONObject

    init {
        scope.launch { dex = loadSD(datapath + "pokedex.ts", Constants.DEXJSONSUB) }
        scope.launch { learnsets = loadSD(datapath + "learnsets.ts", Constants.LEARNSETJSONSUB) }
        scope.launch { moves = loadSD(datapath + "moves.ts", Constants.MOVESJSONSUB) }
        scope.launch { typechart = loadSD(datapath + "typechart.ts", Constants.TYPESJSONSUB) }
        if (name == "default") default = this
        modManagers.add(this)
    }

    companion object {
        private val modManagers = ArrayList<ModManager>()
        private val logger: Logger by SLF4J
        lateinit var default: ModManager
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, t ->
            logger.error("ERROR IN MODMANAGER SCOPE", t)
            Command.sendToMe("Error in modmanager scope, look in console")
        })
    }
}