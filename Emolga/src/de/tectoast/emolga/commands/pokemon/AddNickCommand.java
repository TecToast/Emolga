package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import org.json.JSONObject;

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
        JSONObject json = getEmolgaJSON().getJSONObject("shortcuts");
        ArgumentManager args = e.getArguments();
        String nick = args.getText("nick");
        if (json.has(nick.toLowerCase()) && e.isNotFlo()) {
            e.reply("**" + nick + "** ist bereits als **" + json.getString(nick.toLowerCase()).split(";")[1] + "** hinterlegt!");
            return;
        }
        if(getGerName(nick).isSuccess()) {
            e.reply("Nein.");
            return;
        }
        Translation t = args.getTranslation("stuff");
        json.put(nick.toLowerCase(), t.toOldString());
        e.reply("**" + t.getTranslation() + "** kann ab jetzt auch mit **" + nick + "** abgefragt werden!");
        saveEmolgaJSON();
    }
}
