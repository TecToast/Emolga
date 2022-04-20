package de.tectoast.emolga.commands.music.custom;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.music.GuildMusicManager;
import net.dv8tion.jda.api.entities.VoiceChannel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class MusicQuizCommand extends Command {

    final HashMap<String, String> links = new HashMap<>();

    public MusicQuizCommand() {
        super("musicquiz", "Startet ein Musikquiz von Pokemon im angegebenen Channel", CommandCategory.Flo);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("vid", "Voicechannel-ID", "Die Voicechannel-ID des Channels, wo das Musikquiz stattfinden soll", ArgumentManagerTemplate.DiscordType.ID)
                .add("playlist", "Playlist-Name", "Der Name der Playlist",
                        ArgumentManagerTemplate.Text.of(SubCommand.of("Pokemon"), SubCommand.of("Zelda")))
                .setExample("!musicquiz 744911735705829386 Pokemon")
                .build());
        links.put("Pokemon", "https://www.youtube.com/playlist?list=PLZ0CBSZb0p0ZxmS4x96YeuetEyekhI5oD");
        links.put("Zelda", "https://www.youtube.com/playlist?list=PLrwrdAXSpHC7uEBFqjXGJCNk-hgMD4Fe7");
    }

    @Override
    public void process(GuildCommandEvent e) {
        VoiceChannel vc = e.getJDA().getVoiceChannelById(e.getArguments().getID("vid"));
        vc.getGuild().getAudioManager().openAudioConnection(vc);
        GuildMusicManager musicManager = getGuildAudioPlayer(vc.getGuild());
        String playlistName = e.getArguments().getText("playlist");
        getPlayerManager(vc.getGuild()).loadItemOrdered(musicManager, links.get(playlistName), new AudioLoadResultHandler() {


            @Override
            public void trackLoaded(AudioTrack track) {

            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                ArrayList<AudioTrack> list = new ArrayList<>(playlist.getTracks());
                Collections.shuffle(list);
                int i = 0;
                StringBuilder builder = new StringBuilder();
                for (AudioTrack audioTrack : list) {
                    //logger.info(audioTrack.getInfo().author);
                    if (playlistName.equals("Pokemon") && !audioTrack.getInfo().author.equals("Pokeli")) {
                        continue;
                    }
                    if (i < 20) builder.append(audioTrack.getInfo().title).append("\n");
                    play(vc.getGuild(), musicManager, audioTrack);
                    i++;
                }
                e.reply("Loaded MusicQuiz!");
                e.reply(builder.toString());
            }

            @Override
            public void noMatches() {
                e.reply("NoMatches");
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                exception.printStackTrace();
                e.reply("Der Track konnte nicht abgespielt werden: " + exception.getMessage());
            }
        });
    }
}
