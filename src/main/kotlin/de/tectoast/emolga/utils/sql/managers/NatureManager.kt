package de.tectoast.emolga.utils.sql.managers

import de.tectoast.emolga.utils.sql.base.DataManager
import de.tectoast.emolga.utils.sql.base.DataManager.ResultsFunction
import de.tectoast.emolga.utils.sql.base.columns.StringColumn
import java.sql.ResultSet

object NatureManager : DataManager("natures") {
    private val NAME = StringColumn("name", this)
    private val PLUS = StringColumn("plus", this)
    private val MINUS = StringColumn("minus", this)
    private val statnames = mapOf("atk" to "Atk", "def" to "Def", "spa" to "SpAtk", "spd" to "SpDef", "spe" to "Init")

    init {
        setColumns(NAME, PLUS, MINUS)
    }

    fun getNatureData(str: String): String {
        return read(selectAll(NAME.check(str)), ResultsFunction { s ->
            mapFirst(s, { set: ResultSet ->
                val plus = PLUS.getValue(set)
                val minus = MINUS.getValue(set)
                if (plus != null) {
                    return@mapFirst """
                ${statnames[plus]}+
                ${statnames[minus]}-
                """.trimIndent()
                } else {
                    return@mapFirst "Neutral"
                }
            }, "")
        })
    }
}