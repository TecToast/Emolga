package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.utils.draft.Draft;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class SetupfromfileCommand extends Command {
    public SetupfromfileCommand() {
        super("setupfromfile", "`!setupfromfile <Name> <TCID>`", CommandCategory.Flo);
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
