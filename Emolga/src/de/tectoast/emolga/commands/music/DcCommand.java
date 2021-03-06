package de.tectoast.emolga.commands.music;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.ArrayList;
import java.util.Arrays;

public class DcCommand extends Command {
    public DcCommand() {
        super("dc", "Lässt den Bot disconnecten", CommandCategory.Music);
        overrideChannel.put(712035338846994502L, new ArrayList<>(Arrays.asList(716221567079546983L, 735076688144105493L)));
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        if (tco.getGuild().getId().equals("447357526997073930")) {
            e.getJDA().getGuildById(msg.substring(4)).getAudioManager().closeAudioConnection();
            return;
        }
        tco.getGuild().getAudioManager().closeAudioConnection();
    }
}
