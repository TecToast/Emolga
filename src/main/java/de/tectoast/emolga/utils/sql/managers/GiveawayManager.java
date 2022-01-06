package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.utils.Giveaway;
import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.IntColumn;
import de.tectoast.emolga.utils.sql.base.columns.LongColumn;
import de.tectoast.emolga.utils.sql.base.columns.StringColumn;
import de.tectoast.emolga.utils.sql.base.columns.TimestampColumn;

public class GiveawayManager extends DataManager {

    final LongColumn MESSAGEID = new LongColumn("messageid", this);
    final LongColumn CHANNELID = new LongColumn("channelid", this);
    final LongColumn HOSTID = new LongColumn("hostid", this);
    final StringColumn PRIZE = new StringColumn("prize", this);
    final TimestampColumn END = new TimestampColumn("end", this);
    final IntColumn WINNERS = new IntColumn("winners", this);

    public GiveawayManager() {
        super("giveaways");
        setColumns(MESSAGEID, CHANNELID, HOSTID, PRIZE, END, WINNERS);
    }

    public void saveGiveaway(Giveaway g) {
        insert(g.getMessageId(), g.getChannelId(), g.getUserId(), g.getPrize(), g.getEnd(), g.getWinners());
    }

    public void removeGiveaway(Giveaway g) {
        delete(MESSAGEID.check(g.getMessageId()));
    }
}
