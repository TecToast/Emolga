package de.tectoast.emolga.commands.moderator;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;

public class KickCommand extends Command {
    public KickCommand() {
        super("kick", "Kickt den User", CommandCategory.Moderator);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("user", "User", "Der User, der gekickt werden soll", ArgumentManagerTemplate.DiscordType.USER, true)
                .add("reason", "Grund", "Der Grund des Kicks", ArgumentManagerTemplate.Text.any(), true)
                .setExample("!kick @BöserUser123 Hat böse Sachen gemacht")
                .build()
        );
    }

    @Override
    public void process(GuildCommandEvent e) {
        ArgumentManager args = e.getArguments();
        if (!args.has("user")) return;
        kick(e.getChannel(), e.getMember(), args.getMember("user"), args.getOrDefault("reason", "Nicht angegeben"));
    }
}
