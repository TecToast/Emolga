package de.tectoast.emolga.commands.moderator;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.utils.CommandEvent;
import de.tectoast.emolga.utils.Constants;

import java.util.Arrays;

public class InviteCommand extends Command {
    public InviteCommand() {
        super("invite", "`!invite` Erstellt einen einmalig nutzbaren Invite", CommandCategory.Moderator, Constants.ASLID);
        overrideChannel.put(Constants.ASLID, Arrays.asList(773572093697851392L, 736501675447025704L));
    }

    @Override
    public void process(CommandEvent e) {
        e.getChannel().createInvite().setMaxUses(1).submit().thenCompose(inv -> e.getChannel().sendMessage(inv.getUrl()).submit());
    }
}
