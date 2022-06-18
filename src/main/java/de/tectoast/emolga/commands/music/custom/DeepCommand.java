package de.tectoast.emolga.commands.music.custom;

import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.commands.MusicCommand;
import de.tectoast.emolga.utils.music.GuildMusicManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

public class DeepCommand extends MusicCommand {
    public DeepCommand() {
        super("deep", "Spielt die Deepplaylist ab", 700504340368064562L, 673833176036147210L);
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        Member member = e.getMember();
        Guild g = tco.getGuild();
        GuildMusicManager musicManager = getGuildAudioPlayer(g);
        if (!deep.contains(g)) {
            String url = "https://www.youtube.com/playlist?list=PLaduIcpkVIbrBbU1vxkMSvKdOKo0GJx65";
            deep.add(g);
            loadPlaylist(tco, url, member, ":^)", true);
        } else {
            deep.remove(g);
            musicManager.player.stopTrack();
            musicManager.scheduler.queue.clear();
            tco.sendMessage(":^(").queue();
        }
    }
}
