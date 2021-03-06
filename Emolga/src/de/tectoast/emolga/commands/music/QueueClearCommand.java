package de.tectoast.emolga.commands.music;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.music.GuildMusicManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.ArrayList;
import java.util.Arrays;

public class QueueClearCommand extends Command {
    public QueueClearCommand() {
        super("c", "Cleart die Queue", CommandCategory.Music);
        overrideChannel.put(712035338846994502L, new ArrayList<>(Arrays.asList(716221567079546983L, 735076688144105493L)));
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        GuildMusicManager musicManager = getGuildAudioPlayer(tco.getGuild());
        musicManager.scheduler.queue.clear();
        tco.sendMessage("Die Queue wurde geleert!").queue();
    }
}
