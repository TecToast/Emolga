package de.tectoast.emolga.commands.bs;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;

public class MuffinCommand extends Command {
    public MuffinCommand() {
        super("muffin", "`!muffin` ITS MUFFIN TIME!", CommandCategory.Music);
    }

    @Override
    public void process(GuildCommandEvent e) {
        try {
            loadAndPlay(e.getChannel(), "https://www.youtube.com/watch?v=LACbVhgtx9I", e.getMember(), "**ITS MUFFIN TIME!**");
        } catch (IllegalArgumentException IllegalArgumentException) {
            IllegalArgumentException.printStackTrace();
        }
    }
}
