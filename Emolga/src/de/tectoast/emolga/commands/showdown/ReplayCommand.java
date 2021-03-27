package de.tectoast.emolga.commands.showdown;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.database.Database;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class ReplayCommand extends Command {
    public ReplayCommand() {
        super("replay", "`!replay <Channel>` Schickt von nun an die Ergebnisse aller Replays, die hier rein geschickt werden, in den angegebenen Channel", CommandCategory.Showdown);
        everywhere = true;
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        if (m.getMentionedChannels().size() != 1) {
            tco.sendMessage("Du musst einen Channel angeben!").queue();
            return;
        }
        TextChannel tc = m.getMentionedChannels().get(0);
        Database.insert("analysis", "replay, result", tco.getIdLong(), tc.getIdLong());
        Command.replayAnalysis.put(tco.getIdLong(), tc.getIdLong());
        tco.sendMessage("Die Analyse aus dem Channel " + tco.getAsMention() + " in den Channel " + tc.getAsMention() + " wurde aktiviert!").queue();
        updatePresence();
    }
}
