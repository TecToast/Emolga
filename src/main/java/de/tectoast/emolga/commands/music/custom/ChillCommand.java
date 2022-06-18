package de.tectoast.emolga.commands.music.custom;

import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.commands.MusicCommand;
import de.tectoast.emolga.utils.music.GuildMusicManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

public class ChillCommand extends MusicCommand {
    public ChillCommand() {
        super("chill", "Spielt die Chillplaylist ab", 712035338846994502L, 745934535748747364L);
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        Member member = e.getMember();
        Guild g = tco.getGuild();
        GuildMusicManager musicManager = getGuildAudioPlayer(g);
        if (!chill.contains(g)) {
            String url = "https://www.youtube.com/playlist?list=PLPHBmr2YEhHS17xvYqjt0AgIReBuyAYc2";
            chill.add(g);
            loadPlaylist(tco, url, member, ":^)", true);
        } else {
            chill.remove(g);
            musicManager.player.stopTrack();
            musicManager.scheduler.queue.clear();
            tco.sendMessage(":^(").queue();
        }
    }
}
