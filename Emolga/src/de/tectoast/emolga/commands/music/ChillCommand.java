package de.tectoast.emolga.commands.music;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.utils.CommandEvent;
import de.tectoast.emolga.utils.music.GuildMusicManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.ArrayList;
import java.util.Arrays;

public class ChillCommand extends Command {
    public ChillCommand() {
        super("chill", "`e!chill` Spielt die Chillplaylist ab", CommandCategory.Music, 712035338846994502L);
        overrideChannel.put(712035338846994502L, new ArrayList<>(Arrays.asList(716221567079546983L, 735076688144105493L)));
    }

    @Override
    public void process(CommandEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        Guild g = tco.getGuild();
        GuildMusicManager musicManager = getGuildAudioPlayer(g);
        if (!chill.contains(g)) {
            String url = "https://www.youtube.com/playlist?list=PLPHBmr2YEhHS17xvYqjt0AgIReBuyAYc2";
            chill.add(g);
            loadPlaylist(tco, url, member, ":^)");
        } else {
            chill.remove(g);
            musicManager.player.stopTrack();
            musicManager.scheduler.queue.clear();
            tco.sendMessage(":^(").queue();
        }
    }
}
