package de.tectoast.commands.music;

import de.tectoast.commands.Command;
import de.tectoast.commands.CommandCategory;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class ByeCommand extends Command {
    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        try {
            loadAndPlay(tco, "https://www.youtube.com/watch?v=TgqiSBxvdws", member, ":(");
        } catch (IllegalArgumentException IllegalArgumentException) {
            IllegalArgumentException.printStackTrace();
        }
    }

    public ByeCommand() {
        super("bye", "`e!bye` :^(", CommandCategory.Music, "700504340368064562");
    }
}
