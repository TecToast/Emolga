package de.tectoast.commands.bs;

import de.tectoast.commands.Command;
import de.tectoast.commands.CommandCategory;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class MuffinCommand extends Command {
    public MuffinCommand() {
        super("muffin", "`!muffin` ITS MUFFIN TIME!", CommandCategory.Music);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        try {
            loadAndPlay(e.getChannel(), "https://www.youtube.com/watch?v=LACbVhgtx9I", e.getMember(), "**ITS MUFFIN TIME!**");
        } catch (IllegalArgumentException IllegalArgumentException) {
            IllegalArgumentException.printStackTrace();
        }
    }
}
