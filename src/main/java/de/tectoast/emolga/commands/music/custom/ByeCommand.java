package de.tectoast.emolga.commands.music.custom;

import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.commands.MusicCommand;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class ByeCommand extends MusicCommand {
    public ByeCommand() {
        super("bye", ":^(", 700504340368064562L);
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        try {
            loadAndPlay(tco, "https://www.youtube.com/watch?v=TgqiSBxvdws", member, ":(");
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }
    }
}
