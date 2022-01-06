package de.tectoast.emolga.commands.music.control;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.commands.MusicCommand;
import de.tectoast.emolga.utils.music.GuildMusicManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class NpCommand extends MusicCommand {
    public NpCommand() {
        super("np", "Zeigt, welcher Track gerade läuft");
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        GuildMusicManager manager = getGuildAudioPlayer(tco.getGuild());
        if (manager.player == null) {
            tco.sendMessage("Derzeit läuft kein Track!").queue();
            return;
        }
        AudioPlayer player = manager.player;
        if (player.getPlayingTrack() == null) {
            tco.sendMessage("Derzeit läuft kein Track!").queue();
            return;
        }
        AudioTrack t = player.getPlayingTrack();
        long p = t.getPosition();
        int cm = (int) (p / 60000);
        int cs = (int) ((p - cm * 60000) / 1000);
        long d = t.getDuration();
        int dm = (int) (d / 60000);
        int ds = (int) ((d - dm * 60000) / 1000);
        tco.sendMessage("`" + t.getInfo().title + "`\n" + getWithZeros(cm, 2) + ":" + getWithZeros(cs, 2) + " / " + getWithZeros(dm, 2) + ":" + getWithZeros(ds, 2)).queue();
    }
}
