package de.tectoast.emolga.utils.sql.managers

import de.tectoast.emolga.utils.sql.base.DataManager
import de.tectoast.emolga.utils.sql.base.columns.IntColumn
import de.tectoast.emolga.utils.sql.base.columns.LongColumn
import de.tectoast.emolga.utils.sql.base.columns.StringColumn

object PredictionGameManager : DataManager("predictiongame") {
    private val USERID = LongColumn("userid", this)
    private val USERNAME = StringColumn("username", this)
    private val PREDICTIONS = IntColumn("predictions", this)

    init {
        setColumns(USERID, USERNAME, PREDICTIONS)
    }

    fun addPoint(userid: Long) {
        PREDICTIONS.increment(USERID, userid)
    }
}