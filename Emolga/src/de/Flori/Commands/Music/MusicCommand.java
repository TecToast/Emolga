package de.Flori.Commands.Music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import de.Flori.utils.Music.GuildMusicManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.ArrayList;
import java.util.Random;

public class MusicCommand extends Command {
    public MusicCommand() {
        super("music", "`e!music` Spielt die GamerSquad Playlist ab", CommandCategory.Music, "673833176036147210");
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        Guild g = tco.getGuild();
        GuildMusicManager musicManager = getGuildAudioPlayer(g);
        if (!music.contains(g)) {
            String url = "https://www.youtube.com/playlist?list=PLrwrdAXSpHC5Mr2zC-q_dWKONVybk6JO6";
            music.add(g);
            playerManager.loadItemOrdered(musicManager, url, new AudioLoadResultHandler() {

                @Override
                public void trackLoaded(AudioTrack track) {

                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist) {
                    ArrayList<AudioTrack> list = new ArrayList<>(playlist.getTracks());
                    while (list.size() > 0) {
                        play(g, musicManager, list.remove(new Random().nextInt(list.size())), member, tco);
                    }
                }

                @Override
                public void noMatches() {

                }

                @Override
                public void loadFailed(FriendlyException exception) {
                    tco.sendMessage("Der Track konnte nicht abgespielt werden: " + exception.getMessage()).queue();
                    exception.printStackTrace();
                }
            });
            tco.sendMessage(":^)").queue();
        } else {
            music.remove(g);
            musicManager.player.stopTrack();
            musicManager.scheduler.queue.clear();
            tco.sendMessage(":^(").queue();
        }
    }
}
