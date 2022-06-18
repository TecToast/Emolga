package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.IntColumn;
import de.tectoast.emolga.utils.sql.base.columns.StringColumn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static de.tectoast.emolga.commands.Command.byName;
import static de.tectoast.emolga.commands.Command.updatePresence;

public class StatisticsManager extends DataManager {

    private static final Logger logger = LoggerFactory.getLogger(StatisticsManager.class);

    final StringColumn NAME = new StringColumn("name", this);
    final IntColumn COUNT = new IntColumn("count", this);

    public StatisticsManager() {
        super("statistics");
        setColumns(NAME, COUNT);
    }

    public void increment(String name) {
        COUNT.increment(NAME, name);
        if (name.equals("analysis")) updatePresence();
    }

    public String buildDescription(GuildCommandEvent e) {
        AtomicReference<String> analysis = new AtomicReference<>();
        List<String> otherCmds = new ArrayList<>();
        read(selectBuilder().orderBy(COUNT, "desc").build(this), (ResultsConsumer) s -> forEach(s, set -> {
            int count = COUNT.getValue(set);
            String name = NAME.getValue(set);
            if (name.equals("analysis")) analysis.set("Analysierte Replays: " + count);
            else {
                Command c = byName(name.substring(4));
                if (c != null) {
                    logger.info("name = " + name);
                    if (c.checkBot(e.getJDA(), e.getGuild().getIdLong()))
                        otherCmds.add(c.getPrefix() + c.getName() + ": " + count);
                }
            }
        }));
        return analysis + "\n" + String.join("\n", otherCmds);
    }

    public int getAnalysisCount() {
        return COUNT.retrieveValue(NAME, "analysis");
    }
}
