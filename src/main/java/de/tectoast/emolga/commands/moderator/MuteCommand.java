package de.tectoast.emolga.commands.moderator;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;

public class MuteCommand extends Command {
    public MuteCommand() {
        super("mute", "Mutet den User wegen des angegebenen Grundes", CommandCategory.Moderator);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("user", "User", "Der User, der gemutet werden soll", ArgumentManagerTemplate.DiscordType.USER, true)
                .add("reason", "Grund", "Der Grund des Mutes", ArgumentManagerTemplate.Text.any(), true)
                .setExample("!mute @BöserUser123 Hat böse Wörter verwendet")
                .build()
        );
    }

    @Override
    public void process(GuildCommandEvent e) {
        ArgumentManager args = e.getArguments();
        if (!args.has("user")) return;
        mute(e.getChannel(), e.getMember(), args.getMember("user"), args.getOrDefault("reason", "Nicht angegeben"));
    }
}
