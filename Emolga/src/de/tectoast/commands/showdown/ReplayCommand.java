package de.tectoast.commands.showdown;

import de.tectoast.commands.Command;
import de.tectoast.commands.CommandCategory;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONObject;

public class ReplayCommand extends Command {
    public ReplayCommand() {
        super("replay", "`!replay <Channel>` Schickt von nun an die Ergebnisse aller Replays, die hier rein geschickt werden, in den angegebenen Channel", CommandCategory.Showdown);
        everywhere = true;
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        if (m.getMentionedChannels().size() != 1) {
            tco.sendMessage("Du musst einen Channel angeben!").queue();
            return;
        }
        TextChannel tc = m.getMentionedChannels().get(0);
        JSONObject json = getEmolgaJSON();
        json.getJSONObject("analyse").put(tco.getId(), tc.getId());
        saveEmolgaJSON();
        tco.sendMessage("Die Analyse aus dem Channel " + tco.getAsMention() + " in den Channel " + tc.getAsMention() + " wurde aktiviert!").queue();
    }
}
