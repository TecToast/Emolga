package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.sql.DBManagers;

public class AddNickCommand extends Command {

    public AddNickCommand() {
        super("addnick", "Füge einen Nick für diese Sache hinzu, die ab dann damit abgerufen werden kann", CommandCategory.Pokemon);
        setCustomPermissions(PermissionPreset.CULT);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("nick", "Nickname", "Der neue Nick für die Sache", ArgumentManagerTemplate.Text.any())
                .add("stuff", "Sache", "Pokemon/Item/Whatever", Translation.Type.of(Translation.Type.POKEMON, Translation.Type.MOVE, Translation.Type.ITEM, Translation.Type.ABILITY))
                .setExample("!addnick Banane Manectric")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        ArgumentManager args = e.getArguments();
        String nickorig = args.getText("nick");
        String nick = nickorig.toLowerCase();
        Translation tr = getGerName(nick);
        if (tr.isSuccess() && e.isNotFlo()) {
            e.reply("**" + nickorig + "** ist bereits als **" + tr.getTranslation() + "** hinterlegt!");
            return;
        }
        Translation t = args.getTranslation("stuff");
        DBManagers.TRANSLATIONS.addNick(nick, t);
        e.reply("**" + t.getTranslation() + "** kann ab jetzt auch mit **" + nickorig + "** abgefragt werden!");
        saveEmolgaJSON();
    }
}
