package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.sql.DBManagers;

public class RemoveNickCommand extends Command {

    public RemoveNickCommand() {
        super("removenick", "Removed den Nick", CommandCategory.Pokemon);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("nick", "Nickname", "Der Nickname, der deleted werden soll", ArgumentManagerTemplate.Text.any())
                .setExample("!removenick Schmutz")
                .build());
        setCustomPermissions(PermissionPreset.fromIDs(452575044070277122L, 535095576136515605L));
    }

    @Override
    public void process(GuildCommandEvent e) {
        ArgumentManager args = e.getArguments();
        String nick = args.getText("nick");
        if (DBManagers.TRANSLATIONS.removeNick(nick))
            e.reply("Dieser Nickname wurde entfernt!");
        else
            e.reply("Dieser Nickname existiert nicht!");
    }
}
