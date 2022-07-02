package de.tectoast.emolga.commands.music.custom

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

class MusicQuizCommand :
    Command("musicquiz", "Startet ein Musikquiz von Pokemon im angegebenen Channel", CommandCategory.Flo) {
    private val links = HashMap<String, String>()

    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "vid",
                "Voicechannel-ID",
                "Die Voicechannel-ID des Channels, wo das Musikquiz stattfinden soll",
                ArgumentManagerTemplate.DiscordType.ID
            )
            .add(
                "playlist", "Playlist-Name", "Der Name der Playlist",
                ArgumentManagerTemplate.Text.of(SubCommand.of("Pokemon"), SubCommand.of("Zelda"))
            )
            .setExample("!musicquiz 744911735705829386 Pokemon")
            .build()
        links["Pokemon"] = "https://www.youtube.com/playlist?list=PLZ0CBSZb0p0ZxmS4x96YeuetEyekhI5oD"
        links["Zelda"] = "https://www.youtube.com/playlist?list=PLrwrdAXSpHC7uEBFqjXGJCNk-hgMD4Fe7"
    }

    override fun process(e: GuildCommandEvent) {
        val vc = e.jda.getVoiceChannelById(e.arguments!!.getID("vid"))
        vc!!.guild.audioManager.openAudioConnection(vc)
        val musicManager = getGuildAudioPlayer(vc.guild)
        val playlistName = e.arguments!!.getText("playlist")
        getPlayerManager(vc.guild).loadItemOrdered(musicManager, links[playlistName], object : AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {}
            override fun playlistLoaded(playlist: AudioPlaylist) {
                val list = ArrayList(playlist.tracks)
                list.shuffle()
                var i = 0
                val builder = StringBuilder()
                for (audioTrack in list) {
                    //logger.info(audioTrack.getInfo().author);
                    if (playlistName == "Pokemon" && audioTrack!!.info.author != "Pokeli") {
                        continue
                    }
                    if (i < 20) builder.append(audioTrack!!.info.title).append("\n")
                    play(vc.guild, musicManager, audioTrack)
                    i++
                }
                e.reply("Loaded MusicQuiz!")
                e.reply(builder.toString())
            }

            override fun noMatches() {
                e.reply("NoMatches")
            }

            override fun loadFailed(exception: FriendlyException) {
                exception.printStackTrace()
                e.reply("Der Track konnte nicht abgespielt werden: " + exception.message)
            }
        })
    }
}