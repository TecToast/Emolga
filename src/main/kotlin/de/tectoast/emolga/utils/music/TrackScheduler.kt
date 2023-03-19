package de.tectoast.emolga.utils.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.function.Consumer

class TrackScheduler(private val player: AudioPlayer) : AudioEventAdapter() {
    val queue: BlockingQueue<AudioTrack>
    private val queueLoop = LinkedList<AudioTrack>()
    private val currQueueLoop = LinkedList<AudioTrack>()
    private var loop = false

    init {
        queue = LinkedBlockingQueue()
    }

    /**
     * Add the next track to queue or play right away if nothing is in the queue.
     *
     * @param track The track to play or add to queue.
     */
    fun queue(track: AudioTrack) {
        // Calling startTrack with the noInterrupt set to true will start the track only if nothing is currently playing. If
        // something is playing, it returns false and does nothing. In that case the player was already playing so this
        // track goes to the queue instead.
        if (!player.startTrack(track, true)) {
            queue.offer(track)
        }
    }

    /**
     * Start the next track, stopping the current one if it is playing.
     */
    fun nextTrack() {
        // Start the next track, regardless of if something is already playing or not. In case queue was empty, we are
        // giving null to startTrack, which is a valid argument and will simply stop the player.
        player.startTrack(queue.poll(), false)
    }

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        // Only start the next track if the end reason is suitable for it (FINISHED or LOAD_FAILED)
        if (loop) {
            player.startTrack(track.makeClone(), false)
        } else if (queueLoop.size > 0) {
            if (currQueueLoop.isEmpty()) queueLoop.forEach(Consumer { a: AudioTrack -> currQueueLoop.add(a.makeClone()) })
            player.startTrack(currQueueLoop.removeFirst(), false)
        } else if (endReason.mayStartNext) {
            nextTrack()
        }
    }
}
