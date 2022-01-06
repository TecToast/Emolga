package de.tectoast.emolga.commands.music.control;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.commands.MusicCommand;
import de.tectoast.emolga.utils.music.GuildMusicManager;

public class PauseCommand extends MusicCommand {

    public PauseCommand() {
        super("pause", "Pausiert den derzeitigen Track oder setzt ihn fort, wenn er pausiert ist");
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        GuildMusicManager manager = getGuildAudioPlayer(e.getGuild());
        if (manager.player == null) {
            e.reply("Derzeit läuft kein Track!");
            return;
        }
        AudioPlayer player = manager.player;
        if (player.getPlayingTrack() == null) {
            e.reply("Derzeit läuft kein Track!");
            return;
        }
        player.setPaused(!player.isPaused());
    }
}
