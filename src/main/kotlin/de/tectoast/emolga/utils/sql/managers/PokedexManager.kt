package de.tectoast.emolga.utils.sql.managers

import de.tectoast.emolga.utils.records.DexEntry
import de.tectoast.emolga.utils.sql.base.DataManager
import de.tectoast.emolga.utils.sql.base.DataManager.ResultsFunction
import de.tectoast.emolga.utils.sql.base.columns.StringColumn
import org.slf4j.LoggerFactory
import java.util.*

object PokedexManager : DataManager("pokedex") {
    private val POKEMONNAME = StringColumn("pokemonname", this)
    fun getDexEntry(name: String): DexEntry {
        logger.info(name)
        return read(
            selectAll(
                POKEMONNAME.check(
                    NO_CHARS.replace(name, "").lowercase(Locale.getDefault())
                )
            ), ResultsFunction { set ->
                set.next()
                val possible: MutableList<String> = LinkedList()
                val edis: MutableList<String> = LinkedList()
                val meta = set.metaData
                for (i in 2..37) {
                    val s = set.getString(i)
                    if (s != null) {
                        possible.add(s)
                        edis.add(meta.getColumnName(i))
                    }
                }
                val index = Random().nextInt(possible.size)
                DexEntry(possible[index], edis[index])
            })
    }


    private val logger = LoggerFactory.getLogger(PokedexManager::class.java)
    private val NO_CHARS = Regex("[^A-Za-z]")

}