package de.tectoast.emolga.commands.showdown;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.database.Database;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class ReplayChannelCommand extends Command {
    public ReplayChannelCommand() {
        super("replaychannel", "Schickt von nun an die Ergebnisse aller Replays, die hier rein geschickt werden, in den angegebenen Channel", CommandCategory.Showdown);
        everywhere = true;
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("action", "Aktion", "Die Aktion, die du durchführen möchtest",
                        ArgumentManagerTemplate.Text.of(SubCommand.of("add", "Fügt einen Channel hinzu (Standart, falls dieses Argument weggelassen wird)"), SubCommand.of("remove", "Removed einen Channel")), true)
                .add("channel", "Channel", "Der Channel, wo die Ergebnisse reingeschickt werden sollen", ArgumentManagerTemplate.DiscordType.CHANNEL)
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
        if (m.getMentionedChannels().size() != 1) {
            tco.sendMessage("Du musst einen Channel angeben!").queue();
            return;
        }
        TextChannel tc = m.getMentionedChannels().get(0);
        ArgumentManager args = e.getArguments();
        if (args.has("action") && args.isText("action", "remove")) {
            if (Database.update("DELETE FROM analysis WHERE replay = " + tco.getIdLong() + " AND result = " + tc.getIdLong()) > 0) {
                e.reply("Aus diesem Channel werden keine Ergebnisse mehr in " + tc.getAsMention() + " geschickt!");
            } else {
                e.reply("Zurzeit werden aus diesem Channel keine Replays in " + tc.getAsMention() + " geschickt!");
            }
        } else {
            Database.insert("analysis", "replay, result", tco.getIdLong(), tc.getIdLong());
            Command.replayAnalysis.put(tco.getIdLong(), tc.getIdLong());
            tco.sendMessage("Alle Ergebnisse der Replays aus " + tco.getAsMention() + " werden von nun an in den Channel " + tc.getAsMention() + " geschickt!").queue();
        }
    }
}
