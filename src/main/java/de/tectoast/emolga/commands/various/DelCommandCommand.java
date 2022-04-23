package de.tectoast.emolga.commands.various;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.jsolf.JSONObject;

public class DelCommandCommand extends Command {

    public DelCommandCommand() {
        super("delcommand", "Deleted nen Command oder so", CommandCategory.Flo);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("name", "Name", "Der Name lol", ArgumentManagerTemplate.Text.any())
                .setExample("!delcommand lustigercommandname")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        String name = e.getArguments().getText("name").toLowerCase();
        JSONObject json = getEmolgaJSON().getJSONObject("customcommands");
        if (!json.has(name)) {
            e.reply("Dieser Command existiert nicht!");
            return;
        }
        json.remove(name);
        e.reply("Der Command wurde erfolgreich gelöscht!");
        saveEmolgaJSON();
    }
}
