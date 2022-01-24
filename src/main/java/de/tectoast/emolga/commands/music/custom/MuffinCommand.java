package de.tectoast.emolga.commands.music.custom;

import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.commands.MusicCommand;
import de.tectoast.emolga.utils.Constants;

public class MuffinCommand extends MusicCommand {
    public MuffinCommand() {
        super("muffin", "`!muffin` ITS MUFFIN TIME!", Constants.BSID, Constants.CULTID);
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
