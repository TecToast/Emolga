package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.utils.records.UsageData;
import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.IntColumn;
import de.tectoast.emolga.utils.sql.base.columns.StringColumn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FullStatsManager extends DataManager {

    private static final Logger logger = LoggerFactory.getLogger(FullStatsManager.class);

    final StringColumn POKEMON = new StringColumn("pokemon", this);
    final IntColumn KILLS = new IntColumn("kills", this);
    final IntColumn DEATHS = new IntColumn("deaths", this);
    final IntColumn USES = new IntColumn("uses", this);
    final IntColumn WINS = new IntColumn("wins", this);
    final IntColumn LOOSES = new IntColumn("looses", this);

    public FullStatsManager() {
        super("fullstats");
        setColumns(POKEMON, KILLS, DEATHS, USES, WINS, LOOSES);
    }

    public void add(String pokemon, int kills, int deaths, boolean win) {
        logger.debug("Adding to FSM {} {} {}", pokemon, kills, deaths);
        new Thread(
                () -> addStatistics(pokemon, kills, deaths, 1, win ? 1 : 0, win ? 0 : 1)/*insertOrUpdate(POKEMON, pokemon, r -> {
            r.updateInt("kills", r.getInt("kills") + kills);
            r.updateInt("deaths", r.getInt("deaths") + deaths);
            r.updateInt("uses", r.getInt("uses") + 1);
            String toupdate = win ? "wins" : "looses";
            r.updateInt(toupdate, r.getInt(toupdate) + 1);
        }, pokemon, kills, deaths, 1, win ? 1 : 0, win ? 0 : 1)
                */, "AddFullStat").start();
    }

    public UsageData getData(String mon) {
        return read(selectAll(POKEMON.check(mon)), s -> {
            return mapFirst(s, r -> new UsageData(KILLS.getValue(r), DEATHS.getValue(r), USES.getValue(r), WINS.getValue(r), LOOSES.getValue(r)),
                    new UsageData(0, 0, 0, 0, 0));
        });
    }
}
