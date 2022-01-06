package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.IntColumn;
import de.tectoast.emolga.utils.sql.base.columns.LongColumn;
import de.tectoast.emolga.utils.sql.base.columns.StringColumn;

public class PredictionGameManager extends DataManager {

    final LongColumn USERID = new LongColumn("userid", this);
    final StringColumn USERNAME = new StringColumn("username", this);
    final IntColumn PREDICTIONS = new IntColumn("predictions", this);



    public PredictionGameManager() {
        super("predictiongame");
        setColumns(USERID, USERNAME, PREDICTIONS);
    }
}
