package de.tectoast.emolga.commands.moderator;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.utils.Constants;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.Arrays;

public class InviteCommand extends Command {
    public InviteCommand() {
        super("invite", "`!invite` Erstellt einen einmalig nutzbaren Invite", CommandCategory.Moderator, "518008523653775366");
        overrideChannel.put(Constants.ASLID, Arrays.asList("773572093697851392", "736501675447025704"));
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        e.getChannel().createInvite().setMaxUses(1).submit().thenCompose(inv -> e.getChannel().sendMessage(inv.getUrl()).submit());
    }
}
