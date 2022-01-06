package de.tectoast.emolga.commands.moderator;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;

public class BanCommand extends Command {
    public BanCommand() {
        super("ban", "Bannt den User", CommandCategory.Moderator);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("user", "User", "Der User, der gebannt werden soll", ArgumentManagerTemplate.DiscordType.USER, true)
                .add("reason", "Grund", "Der Grund des Bans", ArgumentManagerTemplate.Text.any(), true)
                .setExample("!ban @BöserUser123 Hat böse Sachen gemacht")
                .build()
        );
    }

    @Override
    public void process(GuildCommandEvent e) {
        ArgumentManager args = e.getArguments();
        if (!args.has("user")) return;
        ban(e.getChannel(), e.getMember(), args.getMember("user"), args.getOrDefault("reason", "Nicht angegeben"));
    }
}
