package de.tectoast.emolga.commands.various;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;

public class InviteUrlCommand extends Command {
    public InviteUrlCommand() {
        super("inviteurl", "Sendet die URL, mit dem man diesen Bot auf andere Server einladen kann", CommandCategory.Various);
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        e.getChannel().sendMessage("https://discord.com/api/oauth2/authorize?client_id=723829878755164202&permissions=8&scope=bot").queue();
    }
}
