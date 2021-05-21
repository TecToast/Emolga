package de.tectoast.emolga.commands.moderator;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;

public class WarnCommand extends Command {
    public WarnCommand() {
        super("warn", "Verwarnt den User", CommandCategory.Moderator);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("user", "User", "Der User, der gekickt werden soll", ArgumentManagerTemplate.DiscordType.USER, true)
                .add("reason", "Grund", "Der Grund des Kicks", ArgumentManagerTemplate.Text.any(), true)
                .setExample("!warn @BöserUser123 Verstoß von Regel XY")
                .build()
        );
    }

    @Override
    public void process(GuildCommandEvent e) {
        ArgumentManager args = e.getArguments();
        warn(e.getChannel(), e.getMember(), args.getMember("user"), args.getOrDefault("reason", "Nicht angegeben"));
    }
}
