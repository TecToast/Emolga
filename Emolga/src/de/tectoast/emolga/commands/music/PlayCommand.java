package de.tectoast.emolga.commands.music;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;

import java.util.ArrayList;
import java.util.Arrays;

public class PlayCommand extends Command {
    public PlayCommand() {
        super("p", "Fügt das Lied der Queue hinzu", CommandCategory.Music);
        aliases.add("play");
        overrideChannel.put(712035338846994502L, new ArrayList<>(Arrays.asList(716221567079546983L, 735076688144105493L)));
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("video", "Link o Suchbegriff", "Der Link bzw. der Suchbegriff für ein YouTube-Video", ArgumentManagerTemplate.Text.any())
                .setExample("e!p https://www.youtube.com/watch?v=dQw4w9WgXcQ")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        try {
            loadAndPlay(e.getChannel(), e.getArguments().getText("video"), e.getMember(), null);
        } catch (IllegalArgumentException IllegalArgumentException) {
            IllegalArgumentException.printStackTrace();
        }
    }
}
