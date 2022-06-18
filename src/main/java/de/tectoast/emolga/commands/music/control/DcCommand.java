package de.tectoast.emolga.commands.music.control;

import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.commands.MusicCommand;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class DcCommand extends MusicCommand {
    public DcCommand() {
        super("dc", "Lässt den Bot disconnecten");
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        if (tco.getGuild().getId().equals("447357526997073930")) {
            e.getJDA().getGuildById(msg.substring(4)).getAudioManager().closeAudioConnection();
            return;
        }
        tco.getGuild().getAudioManager().closeAudioConnection();
    }
}
