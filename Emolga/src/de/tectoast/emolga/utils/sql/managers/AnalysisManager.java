package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.LongColumn;
import net.dv8tion.jda.api.entities.TextChannel;

public class AnalysisManager extends DataManager {

    final LongColumn REPLAY = new LongColumn("replay", this);
    final LongColumn RESULT = new LongColumn("result", this);

    public AnalysisManager() {
        super("analysis");
        setColumns(REPLAY, RESULT);
    }

    public long insertChannel(TextChannel replayChannel, TextChannel resultChannel) {
        Long l = RESULT.retrieveValue(REPLAY, replayChannel.getIdLong());
        if (l != null) {
            return l;
        }
        insert(replayChannel.getIdLong(), resultChannel.getIdLong());
        return -1;
    }
}
