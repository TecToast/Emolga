package de.tectoast.emolga.utils.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TrackScheduler extends AudioEventAdapter {
    public final BlockingQueue<AudioTrack> queue;
    private final AudioPlayer player;
    private final LinkedList<AudioTrack> queueLoop = new LinkedList<>();
    private final LinkedList<AudioTrack> currQueueLoop = new LinkedList<>();
    private boolean loop = false;

    /**
     * @param player The audio player this scheduler uses
     */
    public TrackScheduler(AudioPlayer player) {
        this.player = player;
        this.queue = new LinkedBlockingQueue<>();
    }

    /**
     * Add the next track to queue or play right away if nothing is in the queue.
     *
     * @param track The track to play or add to queue.
     */
    public void queue(AudioTrack track) {
        // Calling startTrack with the noInterrupt set to true will start the track only if nothing is currently playing. If
        // something is playing, it returns false and does nothing. In that case the player was already playing so this
        // track goes to the queue instead.
        if (!player.startTrack(track, true)) {
            //noinspection ResultOfMethodCallIgnored
            queue.offer(track);
        }
    }

    /**
     * Start the next track, stopping the current one if it is playing.
     */
    public void nextTrack() {
        // Start the next track, regardless of if something is already playing or not. In case queue was empty, we are
        // giving null to startTrack, which is a valid argument and will simply stop the player.
        player.startTrack(queue.poll(), false);
    }

    public boolean toggleLoop() {
        return loop = !loop;
    }

    public boolean toggleQueueLoop() {
        if (queueLoop.size() > 0) {
            queueLoop.clear();
            currQueueLoop.clear();
            return false;
        }
        if (player.getPlayingTrack() != null) queueLoop.add(player.getPlayingTrack());
        queueLoop.addAll(queue);
        currQueueLoop.addAll(queue);
        return true;
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        // Only start the next track if the end reason is suitable for it (FINISHED or LOAD_FAILED)
        if (loop) {
            player.startTrack(track.makeClone(), false);
        } else if (queueLoop.size() > 0) {
            if (currQueueLoop.isEmpty()) queueLoop.forEach(a -> currQueueLoop.add(a.makeClone()));
            player.startTrack(currQueueLoop.removeFirst(), false);
        } else if (endReason.mayStartNext) {
            nextTrack();
        }

    }

    public LinkedList<AudioTrack> getQueueLoop() {
        return queueLoop;
    }

    public LinkedList<AudioTrack> getCurrQueueLoop() {
        return currQueueLoop;
    }
}
