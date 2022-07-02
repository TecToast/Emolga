package de.tectoast.emolga.utils

import com.google.api.services.youtube.model.Playlist
import com.google.api.services.youtube.model.SearchResult
import com.google.api.services.youtube.model.Video
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import de.tectoast.emolga.commands.Command.Companion.formatToTime
import de.tectoast.emolga.utils.Google.getPlaylistByURL
import de.tectoast.emolga.utils.Google.getVidByQuery
import de.tectoast.emolga.utils.Google.getVidByURL
import de.tectoast.emolga.utils.music.GuildMusicManager
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageEmbed
import java.awt.Color

class YTDataLoader {
    private val channel: String
    private val thumbnail: String
    val url: String

    private constructor(video: Video?) {
        val sn = video!!.snippet
        channel = sn.channelTitle
        thumbnail = sn.thumbnails.medium.url
        url = "https://www.youtube.com/watch?v=" + video.id
    }

    private constructor(result: SearchResult?) {
        val sn = result!!.snippet
        channel = sn.channelTitle
        thumbnail = sn.thumbnails.medium.url
        url = "https://www.youtube.com/watch?v=" + result.id.videoId
    }

    private constructor(playlist: Playlist?) {
        val sn = playlist!!.snippet
        channel = sn.channelTitle
        thumbnail = sn.thumbnails.medium.url
        url = "https://www.youtube.com/playlist?list=" + playlist.id
    }

    fun buildEmbed(track: AudioTrack, mem: Member, musicManager: GuildMusicManager): MessageEmbed {
        var duration = musicManager.scheduler.queue.stream().mapToLong { obj: AudioTrack -> obj.duration }.sum() / 1000
        if (musicManager.player.playingTrack != null) {
            duration += musicManager.player.playingTrack.duration - musicManager.player.playingTrack.position
        }
        return EmbedBuilder()
            .setTitle(track.info.title, url)
            .setColor(Color.CYAN)
            .setAuthor("Added to queue", null, mem.user.effectiveAvatarUrl)
            .addField("Channel", channel, true)
            .addField("Song Duration", formatToTime(track.duration), true)
            .addField("Estimated time until playing", formatToTime(duration), true)
            .addField(
                "Position in queue",
                (if (musicManager.scheduler.queue.size > 0) musicManager.scheduler.queue.size + 1 else 0).toString(),
                false
            )
            .setThumbnail(thumbnail).build()
    }

    fun buildEmbed(playlist: AudioPlaylist, mem: Member, musicManager: GuildMusicManager): MessageEmbed {
        var duration = musicManager.scheduler.queue.stream().mapToLong { obj: AudioTrack -> obj.duration }.sum() / 1000
        if (musicManager.player.playingTrack != null) {
            duration += musicManager.player.playingTrack.duration - musicManager.player.playingTrack.position
        }
        return EmbedBuilder()
            .setTitle(playlist.name, url)
            .setColor(Color.CYAN)
            .setAuthor("Added to queue", null, mem.user.effectiveAvatarUrl)
            .addField("Channel", channel, true)
            .addField("Estimated time until playing", formatToTime(duration), true)
            .addField(
                "Position in queue",
                (if (musicManager.scheduler.queue.size > 0) musicManager.scheduler.queue.size + 1 else 0).toString(),
                true
            )
            .addField("Queued tracks", playlist.tracks.size.toString(), true)
            .setThumbnail(thumbnail).build()
    }

    companion object {
        private fun fromURL(url: String): YTDataLoader? {
            if (url.startsWith("https://www.youtube.com/playlist?list=")) return YTDataLoader(getPlaylistByURL(url))
            return if (url.startsWith("https://www.youtube.com/watch?v=") || url.startsWith("https://youtu.be/")) YTDataLoader(
                getVidByURL(url)
            ) else null
        }

        fun create(track: String): YTDataLoader? {
            return if (track.startsWith("https://www.youtube.com/") || track.startsWith("https://youtu.be/")) {
                fromURL(track)
            } else {
                YTDataLoader(getVidByQuery(track))
            }
        }
    }
}