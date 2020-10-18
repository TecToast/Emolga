package de.Flori.Commands.BS;

import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class MuffinCommand extends Command {
    public MuffinCommand() {
        super("muffin", "`!muffin` ITS MUFFIN TIME!", CommandCategory.BS);
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
