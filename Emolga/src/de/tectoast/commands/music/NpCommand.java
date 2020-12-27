package de.tectoast.commands.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import de.tectoast.commands.Command;
import de.tectoast.commands.CommandCategory;
import de.tectoast.utils.music.GuildMusicManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.ArrayList;
import java.util.Arrays;

public class NpCommand extends Command {
    public NpCommand() {
        super("np", "`e!np` Zeigt, welcher Track gerade läuft", CommandCategory.Music);
        overrideChannel.put("712035338846994502", new ArrayList<>(Arrays.asList("716221567079546983", "735076688144105493")));
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
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
