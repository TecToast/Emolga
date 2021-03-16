package de.tectoast.emolga.commands.music;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.utils.CommandEvent;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class ByeCommand extends Command {
    @Override
    public void process(CommandEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        try {
            loadAndPlay(tco, "https://www.youtube.com/watch?v=TgqiSBxvdws", member, ":(");
        } catch (IllegalArgumentException IllegalArgumentException) {
            IllegalArgumentException.printStackTrace();
        }
    }

    public ByeCommand() {
        super("bye", "`e!bye` :^(", CommandCategory.Music, 700504340368064562L);
    }
}
