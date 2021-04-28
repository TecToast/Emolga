package de.tectoast.emolga.commands.music;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.music.GuildMusicManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class DeepCommand extends Command {
    public DeepCommand() {
        super("deep", "`e!deep` Spielt die Deepplaylist ab", CommandCategory.Music, 700504340368064562L, 673833176036147210L);
    }

    @Override
    public void process(GuildCommandEvent e) {
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
