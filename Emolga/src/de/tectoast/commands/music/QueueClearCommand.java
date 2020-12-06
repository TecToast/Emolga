package de.tectoast.commands.music;

import de.tectoast.commands.Command;
import de.tectoast.commands.CommandCategory;
import de.tectoast.utils.Music.GuildMusicManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.ArrayList;
import java.util.Arrays;

public class QueueClearCommand extends Command {
    public QueueClearCommand() {
        super("c", "`e!c` Cleart die Queue", CommandCategory.Music);
        overrideChannel.put("712035338846994502", new ArrayList<>(Arrays.asList("716221567079546983", "735076688144105493")));
    }

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
}
