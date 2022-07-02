package de.tectoast.emolga.utils.sql.managers

import de.tectoast.emolga.utils.records.NGData
import de.tectoast.emolga.utils.sql.base.DataManager
import de.tectoast.emolga.utils.sql.base.DataManager.ResultsFunction
import de.tectoast.emolga.utils.sql.base.columns.IntColumn
import de.tectoast.emolga.utils.sql.base.columns.StringColumn
import java.sql.ResultSet

object NaturalGiftManager : DataManager("naturalgift") {
    private val NAME = StringColumn("name", this)
    private val TYPE = StringColumn("type", this)
    private val BP = IntColumn("bp", this)

    init {
        setColumns(NAME, TYPE, BP)
    }

    fun fromName(name: String): NGData {
        return read(selectAll(NAME.check(name)), ResultsFunction { s ->
            mapFirst(s) { set: ResultSet ->
                NGData(
                    name, TYPE.getValue(
                        set
                    ), BP.getValue(set)
                )
            }
        })
    }

    fun fromType(type: String): List<NGData> {
        return read(selectAll(TYPE.check(type)), ResultsFunction { s ->
            map(s) { set: ResultSet ->
                NGData(
                    NAME.getValue(
                        set
                    ), type, BP.getValue(set)
                )
            }
        })
    }
}