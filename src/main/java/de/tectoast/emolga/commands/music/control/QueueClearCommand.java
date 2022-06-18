package de.tectoast.emolga.commands.music.control;

import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.commands.MusicCommand;
import de.tectoast.emolga.utils.music.GuildMusicManager;
import net.dv8tion.jda.api.entities.TextChannel;

public class QueueClearCommand extends MusicCommand {
    public QueueClearCommand() {
        super("c", "Cleart die Queue");
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        GuildMusicManager musicManager = getGuildAudioPlayer(tco.getGuild());
        musicManager.scheduler.queue.clear();
        tco.sendMessage("Die Queue wurde geleert!").queue();
    }
}
