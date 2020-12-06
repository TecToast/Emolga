package de.tectoast.commands.music;

import de.tectoast.commands.Command;
import de.tectoast.commands.CommandCategory;
import de.tectoast.utils.Music.GuildMusicManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class DeepCommand extends Command {
    public DeepCommand() {
        super("deep", "`e!deep` Spielt die Deepplaylist ab", CommandCategory.Music, "700504340368064562");
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        Guild g = tco.getGuild();
        GuildMusicManager musicManager = getGuildAudioPlayer(g);
        if (!deep.contains(g)) {
            String url = "https://www.youtube.com/playlist?list=PLaduIcpkVIbrBbU1vxkMSvKdOKo0GJx65";
            deep.add(g);
            loadPlaylist(tco, url, member, ":^)");
        } else {
            deep.remove(g);
            musicManager.player.stopTrack();
            musicManager.scheduler.queue.clear();
            tco.sendMessage(":^(").queue();
        }
    }
}
