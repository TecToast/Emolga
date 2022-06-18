package de.tectoast.emolga.commands.showdown;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.sql.DBManagers;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.List;

public class ReplayChannelCommand extends Command {
    public ReplayChannelCommand() {
        super("replaychannel", "Schickt von nun an die Ergebnisse aller Replays, die in diesen Channel geschickt werden, in den angegebenen Channel (wenn die Ergebnisse in den gleichen Channel sollen, tagge einfach diesen Channel hier)", CommandCategory.Showdown);
        everywhere = true;
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("action", "Aktion", "Die Aktion, die du durchführen möchtest",
                        ArgumentManagerTemplate.Text.of(SubCommand.of("add", "Fügt einen Channel hinzu (Standard, falls dieses Argument weggelassen wird)"), SubCommand.of("remove", "Removed einen Channel")), true)
                .add("channel", "Channel", "Der Channel, wo die Ergebnisse reingeschickt werden sollen", ArgumentManagerTemplate.DiscordType.CHANNEL, true)
                .setExample("!replaychannel #ergebnisse-emolga")
                .build());
        aliases.add("replay");
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        if (e.getUsedName().equals("replay")) {
            sendToUser(e.getAuthor(), "Der Command wurde in !replaychannel umbenannt, damit er sich nicht mehr mit anderen Bots schneidet. !replay funktioniert weiterhin, jedoch sollte am besten !replaychannel verwendet werden.");
        }
        List<TextChannel> channels = m.getMentions().getChannels(TextChannel.class);
        boolean sameChannel = channels.size() == 0;
        TextChannel tc = sameChannel ? tco : channels.get(0);
        ArgumentManager args = e.getArguments();
        if (args.has("action") && args.isText("action", "remove")) {
            if (DBManagers.ANALYSIS.deleteChannel(tco.getIdLong())) {
                e.reply("Dieser Channel ist nun kein Replaychannel mehr!");
                replayAnalysis.remove(tco.getIdLong());
            } else {
                e.reply("Dieser Channel ist zurzeit kein Replaychannel!");
            }
        } else {
            //Database.insert("analysis", "replay, result", tco.getIdLong(), tc.getIdLong());
            long l = DBManagers.ANALYSIS.insertChannel(tco, tc);
            if (l == -1) {
                e.reply(sameChannel ? "Dieser Channel ist nun ein Replaychannel, somit werden alle Replay-Ergebnisse automatisch hier reingeschickt!" : ("Alle Ergebnisse der Replays aus " + tco.getAsMention() + " werden von nun an in den Channel " + tc.getAsMention() + " geschickt!"));
                Command.replayAnalysis.put(tco.getIdLong(), tc.getIdLong());
            } else {
                e.reply("Die Replays aus diesem Channel werden " + (l == tc.getIdLong() ? "bereits" : "zurzeit") + " in den Channel <#" + l + "> geschickt!");
            }
        }
    }
}
