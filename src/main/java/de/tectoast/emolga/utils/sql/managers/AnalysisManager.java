package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.bot.EmolgaMain;
import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.LongColumn;
import net.dv8tion.jda.api.entities.TextChannel;

public class AnalysisManager extends DataManager {

    final LongColumn REPLAY = new LongColumn("replay", this);
    final LongColumn RESULT = new LongColumn("result", this);
    final LongColumn GUILD = new LongColumn("guild", this);

    public AnalysisManager() {
        super("analysis");
        setColumns(REPLAY, RESULT, GUILD);
    }

    public long insertChannel(TextChannel replayChannel, TextChannel resultChannel) {
        long l = RESULT.retrieveValue(REPLAY, replayChannel.getIdLong());
        if (l != -1) {
            return l;
        }
        insert(replayChannel.getIdLong(), resultChannel.getIdLong(), replayChannel.getGuild().getIdLong());
        return -1;
    }

    public int removeUnused() {
        return readWrite(selectAll(), r -> {
            int x = 0;
            while (r.next()) {
                if (EmolgaMain.emolgajda.getTextChannelById(r.getLong("replay")) == null) {
                    r.deleteRow();
                    x++;
                }
            }
            return x;
        });
    }
}
