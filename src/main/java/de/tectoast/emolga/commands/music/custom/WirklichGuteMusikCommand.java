package de.tectoast.emolga.commands.music.custom;

import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.commands.MusicCommand;
import de.tectoast.emolga.utils.Constants;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

public class WirklichGuteMusikCommand extends MusicCommand {
    public WirklichGuteMusikCommand() {
        super("gutemusik", "Wirklich Gute Musik (Empfohlen von Flo und Dasor :) )", Constants.FPLID);
    }

    public static void doIt(TextChannel tc, Member mem, boolean good) {
        try {
            loadAndPlay(tc, good ? "https://www.youtube.com/watch?v=4Diu2N8TGKA" : "https://www.youtube.com/watch?v=dQw4w9WgXcQ", mem, "**ITS GUTE MUSIK TIME!**");
            getGuildAudioPlayer(tc.getGuild()).scheduler.enableLoop();
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void process(GuildCommandEvent e) {
        doIt(e.getChannel(), e.getMember(), true);
    }
}
