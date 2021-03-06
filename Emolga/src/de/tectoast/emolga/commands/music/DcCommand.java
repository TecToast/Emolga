package de.tectoast.emolga.commands.music;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.bot.EmolgaMain;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.ArrayList;
import java.util.Arrays;

public class DcCommand extends Command {
    public DcCommand() {
        super("dc", "`e!dc` Lässt den Bot disconnecten", CommandCategory.Music);
        overrideChannel.put("712035338846994502", new ArrayList<>(Arrays.asList("716221567079546983", "735076688144105493")));
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        if (tco.getGuild().getId().equals("447357526997073930")) {
            EmolgaMain.jda.getGuildById(msg.substring(4)).getAudioManager().closeAudioConnection();
            return;
        }
        tco.getGuild().getAudioManager().closeAudioConnection();
    }
}
