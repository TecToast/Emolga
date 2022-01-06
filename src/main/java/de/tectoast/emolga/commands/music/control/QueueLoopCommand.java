package de.tectoast.emolga.commands.music.control;

import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.commands.MusicCommand;
import de.tectoast.emolga.utils.music.GuildMusicManager;

public class QueueLoopCommand extends MusicCommand {

    public QueueLoopCommand() {
        super("queueloop", "Loopt die derzeitige Queue oder beendet die Loop");
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        GuildMusicManager manager = getGuildAudioPlayer(e.getGuild());
        e.reply("QueueLoop wurde " + (manager.scheduler.toggleQueueLoop() ? "" : "de") + "aktiviert!");
    }
}
