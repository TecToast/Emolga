package de.tectoast.emolga.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.music.GuildMusicManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.ArrayList;
import java.util.Arrays;

public class QueueCommand extends Command {
    public QueueCommand() {
        super("q", "Zeigt die Queue an", CommandCategory.Music);
        aliases.add("queue");
        overrideChannel.put(712035338846994502L, new ArrayList<>(Arrays.asList(716221567079546983L, 735076688144105493L)));
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
        str.append("Gerade: `").append(musicManager.player.getPlayingTrack().getInfo().title).append("`\n");
        int i = 1;
        for (AudioTrack audioTrack : musicManager.scheduler.queue) {
            str.append(getWithZeros(i, 2)).append(": `").append(audioTrack.getInfo().title).append("`\n");
            if (str.length() > 1900) {
                tco.sendMessage(str.toString()).queue();
                str = new StringBuilder();
            }
            i++;
        }
        tco.sendMessage(str.toString()).queue();
    }
}
