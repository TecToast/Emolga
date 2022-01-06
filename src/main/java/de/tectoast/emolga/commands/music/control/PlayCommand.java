package de.tectoast.emolga.commands.music.control;

import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.commands.MusicCommand;

public class PlayCommand extends MusicCommand {
    public PlayCommand() {
        super("p", "Fügt das Lied der Queue hinzu");
        aliases.add("play");
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
