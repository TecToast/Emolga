package de.Flori.Commands.Music;

import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class PlayCommand extends Command {
    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        String str;
        if (msg.startsWith("e!play"))
            str = msg.substring(7);
        else
            str = msg.substring(4);
        try {
            loadAndPlay(tco, str, member, null);
        } catch (IllegalArgumentException IllegalArgumentException) {
            IllegalArgumentException.printStackTrace();
        }
    }

    public PlayCommand() {
        super("p", "`e!p <Link|Suchbegriff>` Fügt das Lied der Queue hinzu", CommandCategory.Music);
        aliases.add("play");
        overrideChannel.put("712035338846994502", new ArrayList<>(Arrays.asList("716221567079546983", "735076688144105493")));
    }
}
