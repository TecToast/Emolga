package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.IntColumn;
import de.tectoast.emolga.utils.sql.base.columns.StringColumn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FullStatsManager extends DataManager {

    private static final Logger logger = LoggerFactory.getLogger(FullStatsManager.class);

    StringColumn POKEMON = new StringColumn("pokemon", this);
    IntColumn KILLS = new IntColumn("kills", this);
    IntColumn DEATHS = new IntColumn("deaths", this);
    IntColumn USES = new IntColumn("uses", this);
    IntColumn WINS = new IntColumn("wins", this);
    IntColumn LOOSES = new IntColumn("looses", this);

    public FullStatsManager() {
        super("fullstats");
        setColumns(POKEMON, KILLS, DEATHS, USES, WINS, LOOSES);
    }

    public void add(String pokemon, int kills, int deaths, boolean win) {
        logger.debug("Adding to FSM {} {} {}", pokemon, kills, deaths);
        new Thread(() -> insertOrUpdate(POKEMON, pokemon, r -> {
            r.updateInt("kills", r.getInt("kills") + kills);
            r.updateInt("deaths", r.getInt("deaths") + deaths);
            r.updateInt("uses", r.getInt("uses") + 1);
            String toupdate = win ? "wins" : "looses";
            r.updateInt(toupdate, r.getInt(toupdate) + 1);
        }, pokemon, kills, deaths, 1, win ? 1 : 0, win ? 0 : 1), "AddFullStat").start();
    }
}
