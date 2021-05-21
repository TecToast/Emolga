package de.tectoast.emolga.commands.moderator;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.Constants;

import java.util.Arrays;

public class InviteCommand extends Command {
    public InviteCommand() {
        super("invite", "Erstellt einen einmalig nutzbaren Invite", CommandCategory.Moderator, Constants.ASLID);
        overrideChannel.put(Constants.ASLID, Arrays.asList(773572093697851392L, 736501675447025704L));
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        e.getChannel().createInvite().setMaxUses(1).flatMap(inv -> e.getChannel().sendMessage(inv.getUrl())).queue();
    }
}
