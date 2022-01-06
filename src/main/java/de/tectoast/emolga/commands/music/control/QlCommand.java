package de.tectoast.emolga.commands.music.control;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.commands.MusicCommand;
import de.tectoast.emolga.utils.music.GuildMusicManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class QlCommand extends MusicCommand {
    public QlCommand() {
        super("ql", "Zeigt die Länge der Queue an");
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        GuildMusicManager musicManager = getGuildAudioPlayer(tco.getGuild());
        long duration = musicManager.scheduler.queue.stream().mapToLong(AudioTrack::getDuration).sum() / 1000;
        int hours = (int) (duration / 3600);
        int minutes = (int) ((duration - hours * 3600) / 60);
        int seconds = (int) (duration - hours * 3600 - minutes * 60);
        String str = "";
        if (hours > 0) str += hours + "h ";
        str += minutes + "m ";
        str += seconds + "s";
        tco.sendMessage(str).queue();
    }
}
