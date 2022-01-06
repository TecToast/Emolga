package de.tectoast.emolga.utils;

import com.google.api.services.youtube.model.*;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import de.tectoast.emolga.utils.music.GuildMusicManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;

import static de.tectoast.emolga.commands.Command.formatToTime;

public class YTDataLoader {
    private final String channel;
    private final String thumbnail;
    private final String url;

    private YTDataLoader(Video video) {
        VideoSnippet sn = video.getSnippet();
        this.channel = sn.getChannelTitle();
        this.thumbnail = sn.getThumbnails().getMedium().getUrl();
        this.url = "https://www.youtube.com/watch?v=" + video.getId();
    }

    private YTDataLoader(SearchResult result) {
        SearchResultSnippet sn = result.getSnippet();
        this.channel = sn.getChannelTitle();
        this.thumbnail = sn.getThumbnails().getMedium().getUrl();
        this.url = "https://www.youtube.com/watch?v=" + result.getId().getVideoId();
    }

    private YTDataLoader(Playlist playlist) {
        PlaylistSnippet sn = playlist.getSnippet();
        this.channel = sn.getChannelTitle();
        this.thumbnail = sn.getThumbnails().getMedium().getUrl();
        this.url = "https://www.youtube.com/playlist?list=" + playlist.getId();
    }

    private static YTDataLoader fromURL(String url) {
        if (url.startsWith("https://www.youtube.com/playlist?list="))
            return new YTDataLoader(Google.getPlaylistByURL(url));
        if (url.startsWith("https://www.youtube.com/watch?v=") || url.startsWith("https://youtu.be/"))
            return new YTDataLoader(Google.getVidByURL(url));
        return null;
    }

    public static YTDataLoader create(String track) {
        if (track.startsWith("https://www.youtube.com/") || track.startsWith("https://youtu.be/")) {
            return fromURL(track);
        } else {
            return new YTDataLoader(Google.getVidByQuery(track));
        }
    }

    public String getChannel() {
        return channel;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public String getUrl() {
        return url;
    }

    public MessageEmbed buildEmbed(AudioTrack track, Member mem, GuildMusicManager musicManager) {
        long duration = musicManager.scheduler.queue.stream().mapToLong(AudioTrack::getDuration).sum() / 1000;
        if (musicManager.player.getPlayingTrack() != null) {
            duration += (musicManager.player.getPlayingTrack().getDuration() - musicManager.player.getPlayingTrack().getPosition());
        }
        return new EmbedBuilder()
                .setTitle(track.getInfo().title, url)
                .setColor(Color.CYAN)
                .setAuthor("Added to queue", null, mem.getUser().getEffectiveAvatarUrl())
                .addField("Channel", getChannel(), true)
                .addField("Song Duration", formatToTime(track.getDuration()), true)
                .addField("Estimated time until playing", formatToTime(duration), true)
                .addField("Position in queue", (musicManager.scheduler.queue.size() > 0 ? musicManager.scheduler.queue.size() + 1 : 0) + "", false)
                .setThumbnail(getThumbnail()).build();
    }

    public MessageEmbed buildEmbed(AudioPlaylist playlist, Member mem, GuildMusicManager musicManager) {
        long duration = musicManager.scheduler.queue.stream().mapToLong(AudioTrack::getDuration).sum() / 1000;
        if (musicManager.player.getPlayingTrack() != null) {
            duration += (musicManager.player.getPlayingTrack().getDuration() - musicManager.player.getPlayingTrack().getPosition());
        }
        return new EmbedBuilder()
                .setTitle(playlist.getName(), url)
                .setColor(Color.CYAN)
                .setAuthor("Added to queue", null, mem.getUser().getEffectiveAvatarUrl())
                .addField("Channel", getChannel(), true)
                .addField("Estimated time until playing", formatToTime(duration), true)
                .addField("Position in queue", (musicManager.scheduler.queue.size() > 0 ? musicManager.scheduler.queue.size() + 1 : 0) + "", true)
                .addField("Queued tracks", String.valueOf(playlist.getTracks().size()), true)
                .setThumbnail(getThumbnail()).build();
    }
}
