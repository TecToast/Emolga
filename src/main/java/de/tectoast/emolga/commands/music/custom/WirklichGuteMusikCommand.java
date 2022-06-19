package de.tectoast.emolga.commands.music.custom;

import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.commands.MusicCommand;
import de.tectoast.emolga.utils.Constants;

public class WirklichGuteMusikCommand extends MusicCommand {
    public WirklichGuteMusikCommand() {
        super("gutemusik", "Wirklich Gute Musik (Empfohlen von Flo und Dasor :) )", Constants.FPLID);
    }

    @Override
    public void process(GuildCommandEvent e) {
        try {
            loadAndPlay(e.getChannel(), "https://www.youtube.com/watch?v=4Diu2N8TGKA", e.getMember(), "**ITS GUTE MUSIK TIME!**");
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }
    }
}
