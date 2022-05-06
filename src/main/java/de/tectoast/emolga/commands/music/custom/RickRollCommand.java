package de.tectoast.emolga.commands.music.custom;

import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.commands.MusicCommand;

public class RickRollCommand extends MusicCommand {
    public RickRollCommand() {
        super("rickroll", ":^)");
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) throws Exception {
        loadAndPlay(e.getChannel(), "https://www.youtube.com/watch?v=dQw4w9WgXcQ", e.getMember(), null);
        e.reply(":^)");
    }
}
