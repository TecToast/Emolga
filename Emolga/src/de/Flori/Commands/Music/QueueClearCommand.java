package de.Flori.Commands.Music;

import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import de.Flori.utils.Music.GuildMusicManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.ArrayList;
import java.util.Arrays;

public class QueueClearCommand extends Command {
    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        GuildMusicManager musicManager = getGuildAudioPlayer(tco.getGuild());
        musicManager.scheduler.queue.clear();
        tco.sendMessage("Die Queue wurde geleert!").queue();
    }

    public QueueClearCommand() {
        super("c", "`e!c` Cleart die Queue", CommandCategory.Music);
        overrideChannel.put("712035338846994502", new ArrayList<>(Arrays.asList("716221567079546983", "735076688144105493")));
    }
}
