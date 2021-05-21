package de.tectoast.emolga.commands.music;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;

import java.util.ArrayList;
import java.util.Arrays;

public class SkipCommand extends Command {
    public SkipCommand() {
        super("s", "Skippt den derzeitigen Track", CommandCategory.Music);
        aliases.add("skip");
        overrideChannel.put(712035338846994502L, new ArrayList<>(Arrays.asList(716221567079546983L, 735076688144105493L)));
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        skipTrack(e.getChannel());
    }
}
