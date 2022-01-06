package de.tectoast.emolga.commands.music.control;

import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.commands.MusicCommand;
import de.tectoast.emolga.utils.music.GuildMusicManager;

public class LoopCommand extends MusicCommand {

    public LoopCommand() {
        super("loop", "Loopt den derzeitigen Track oder beendet die Loop");
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        GuildMusicManager manager = getGuildAudioPlayer(e.getGuild());
        e.reply("Loop wurde " + (manager.scheduler.toggleLoop() ? "" : "de") + "aktiviert!");
    }
}
