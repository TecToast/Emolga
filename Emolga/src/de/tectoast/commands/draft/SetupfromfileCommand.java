package de.tectoast.commands.draft;

import de.tectoast.commands.Command;
import de.tectoast.commands.CommandCategory;
import de.tectoast.utils.Draft.Draft;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class SetupfromfileCommand extends Command {
    public SetupfromfileCommand() {
        super("setupfromfile", "`!setupfromfile <Name> <TCID>`", CommandCategory.Flo, true);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        String name = msg.split(" ")[1];
        String tcid = m.getMentionedChannels().get(0).getId();
        m.delete().queue();
        new Draft(tco, name, tcid, true);
    }
}
