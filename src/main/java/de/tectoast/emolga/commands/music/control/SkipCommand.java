package de.tectoast.emolga.commands.music.control;

import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.commands.MusicCommand;

public class SkipCommand extends MusicCommand {
    public SkipCommand() {
        super("s", "Skippt den derzeitigen Track");
        aliases.add("skip");
        addCustomChannel(712035338846994502L, 716221567079546983L, 735076688144105493L);
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        skipTrack(e.getChannel());
    }
}
