package de.tectoast.emolga.utils

import de.tectoast.emolga.commands.Command.Companion.loadSD
import de.tectoast.jsolf.JSONObject

class ModManager(private val name: String, datapath: String) {
    lateinit var dex: JSONObject
    lateinit var learnsets: JSONObject
    lateinit var moves: JSONObject
    lateinit var typechart: JSONObject

    init {
        Thread({ dex = loadSD(datapath + "pokedex.ts", Constants.DEXJSONSUB) }, "ModManager $name Dex").start()
        Thread(
            { learnsets = loadSD(datapath + "learnsets.ts", Constants.LEARNSETJSONSUB) },
            "ModManager $name Learnsets"
        ).start()
        Thread({ moves = loadSD(datapath + "moves.ts", Constants.MOVESJSONSUB) }, "ModManager $name Moves").start()
        Thread(
            { typechart = loadSD(datapath + "typechart.ts", Constants.TYPESJSONSUB) },
            "ModManager $name Typechart"
        ).start()
        if (name == "default") default = this
        modManagers.add(this)
    }

    companion object {
        private val modManagers = ArrayList<ModManager>()
        lateinit var default: ModManager
        fun getByName(name: String): ModManager {
            return modManagers.stream().filter { m: ModManager? -> m!!.name == name }.findFirst().orElse(null)
        }
    }
}