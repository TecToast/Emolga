package de.tectoast.emolga.commands.flegmon;

import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.commands.PepeCommand;

public class OddsCommand extends PepeCommand {

    public OddsCommand() {
        super("odds", "Bin zu faul Help Nachrichten zu schreiben");
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        long uid = e.getAuthor().getIdLong();
        if (uid == 322755315953172485L) {
            e.reply("hm, joa, das sind zu viele Nullen nach dem Komma zum zählen :c");
        } else {
            e.reply("Die Chance beträgt 1/3.");
        }
    }
}
