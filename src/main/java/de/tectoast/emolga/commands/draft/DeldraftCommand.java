package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import org.jsolf.JSONObject;

public class DeldraftCommand extends Command {
    public DeldraftCommand() {
        super("deldraft", "Löscht den Draft mit dem angegebenen Namen", CommandCategory.Flo);
        setArgumentTemplate(ArgumentManagerTemplate.builder().add("name", "Name", "Name des Drafts", ArgumentManagerTemplate.draft())
                .setExample("!deldraft Emolga-Conference")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        String name = e.getArguments().getText("name");
        JSONObject json = getEmolgaJSON();
        if (json.has("drafts")) {
            JSONObject drafts = json.getJSONObject("drafts");
            if (drafts.has(name)) {
                drafts.remove(name);
                saveEmolgaJSON();
                e.reply("Dieser Draft wurde gelöscht!");
            } else {
                e.reply("Dieser Draft existiert nicht!");
            }
        } else {
            e.reply("Es wurde noch kein Draft erstellt!");
        }
    }
}
