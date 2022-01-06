package de.tectoast.emolga.commands.moderator;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;

public class UnmuteCommand extends Command {
    public UnmuteCommand() {
        super("unmute", "Entmutet den User", CommandCategory.Moderator);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("user", "User", "User, der entmutet werden soll", ArgumentManagerTemplate.DiscordType.USER)
                .setExample("!unmute @BöserUser123")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        unmute(e.getChannel(), e.getArguments().getMember("user"));
    }
}
