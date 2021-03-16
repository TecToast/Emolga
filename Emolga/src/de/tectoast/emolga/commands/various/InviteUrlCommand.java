package de.tectoast.emolga.commands.various;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.utils.CommandEvent;

public class InviteUrlCommand extends Command {
    public InviteUrlCommand() {
        super("inviteurl", "`!inviteurl` Sendet die URL, mit dem man diesen Bot auf andere Server einladen kann", CommandCategory.Various);
    }

    @Override
    public void process(CommandEvent e) {
        e.getChannel().sendMessage("https://discord.com/api/oauth2/authorize?client_id=723829878755164202&permissions=8&scope=bot").queue();
    }
}
