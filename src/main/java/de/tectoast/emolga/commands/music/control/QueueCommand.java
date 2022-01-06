package de.tectoast.emolga.commands.music.control;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.commands.MusicCommand;
import de.tectoast.emolga.utils.music.GuildMusicManager;
import de.tectoast.emolga.utils.music.TrackScheduler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.LinkedList;

public class QueueCommand extends MusicCommand {
    public QueueCommand() {
        super("q", "Zeigt die Queue an");
        aliases.add("queue");
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        Guild g = tco.getGuild();
        GuildMusicManager musicManager = getGuildAudioPlayer(g);
        StringBuilder str = new StringBuilder();
        if (musicManager.player.getPlayingTrack() == null) {
            tco.sendMessage("Derzeit ist kein Lied in der Queue!").queue();
            return;
        }
        TrackScheduler sched = musicManager.scheduler;
        LinkedList<AudioTrack> queueLoop = sched.getQueueLoop();
        if (queueLoop.size() > 0) {
            int i = 1;
            int num = queueLoop.size() - sched.getCurrQueueLoop().size();
            for (AudioTrack audioTrack : queueLoop) {
                if (i == num) str.append("**");
                str.append(getWithZeros(i, 2));
                if (i == num) str.append("**");
                str.append(": `").append(audioTrack.getInfo().title).append("`\n");
                if (str.length() > 1900) {
                    tco.sendMessage(str.toString()).queue();
                    str = new StringBuilder();
                }
                i++;
            }
        } else {
            str.append("Gerade: `").append(musicManager.player.getPlayingTrack().getInfo().title).append("`\n");
            int i = 1;
            for (AudioTrack audioTrack : sched.queue) {
                str.append(getWithZeros(i, 2)).append(": `").append(audioTrack.getInfo().title).append("`\n");
                if (str.length() > 1900) {
                    tco.sendMessage(str.toString()).queue();
                    str = new StringBuilder();
                }
                i++;
            }
        }
        tco.sendMessage(str.toString()).queue();
    }
}
