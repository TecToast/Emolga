package de.Flori.Commands.Admin;

import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import de.Flori.Commands.Giveaway.GcreateCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;

import static de.Flori.Commands.Giveaway.GcreateCommand.secondsToTime;

public class TempBanCommand extends Command {
    public TempBanCommand() {
        super("tempban", "`!tempban <User> <Zeit> <Grund>", CommandCategory.Admin, "712035338846994502");
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        Message m = e.getMessage();
        String raw = m.getContentRaw();
        TextChannel tco = e.getChannel();
        if (m.getMentionedMembers().size() != 1) {
            //tco.sendMessage("Du musst einen Spieler taggen!").queue();
            return;
        }
        Member mem = m.getMentionedMembers().get(0);
        String[] splitarr = raw.split(" ");
        //ArrayList<String> split = new ArrayList<>(Arrays.asList(splitarr));
        StringBuilder reasonbuilder = new StringBuilder();
        int time = 0;
        for (int i = 2; i < splitarr.length; i++) {
            if (GcreateCommand.parseShortTime(splitarr[i]) != -1) {
                time += GcreateCommand.parseShortTime(splitarr[i]);
            } else reasonbuilder.append(splitarr[i]).append(" ");
        }
        String reason = reasonbuilder.toString().trim().equals("") ? "Nicht angegeben" : reasonbuilder.toString().trim();
        tempBan(tco, e.getMember(), mem, time, reason);
    }
}
