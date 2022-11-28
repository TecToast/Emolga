package de.tectoast.emolga.utils

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.Command.Companion.load
import de.tectoast.emolga.utils.json.showdown.Learnset
import de.tectoast.emolga.utils.json.showdown.Pokemon
import de.tectoast.emolga.utils.json.showdown.TypeData
import dev.minn.jda.ktx.util.SLF4J
import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonObject
import org.slf4j.Logger

class ModManager(name: String, datapath: String) {
    lateinit var dex: Map<String, Pokemon>
    lateinit var learnsets: Map<String, Learnset>
    lateinit var moves: JsonObject
    lateinit var typechart: Map<String, TypeData>

    init {
        scope.launch { dex = load(datapath + "pokedex.json") }
        scope.launch { learnsets = load(datapath + "learnsets.json") }
        scope.launch { moves = load(datapath + "moves.json") }
        scope.launch { typechart = load(datapath + "typechart.json") }
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
