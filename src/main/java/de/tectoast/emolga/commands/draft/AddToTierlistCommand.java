package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.Constants;
import de.tectoast.emolga.utils.draft.Tierlist;
import org.jsolf.JSONObject;

public class AddToTierlistCommand extends Command {

    public AddToTierlistCommand() {
        super("addtotierlist", "Fügt ein Mon ins D-Tier ein", CommandCategory.Draft, Constants.ASLID);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("mon", "Mon", "Das Mon", ArgumentManagerTemplate.Text.any(), false)
                .setExample("!addtotierlist Chimstix")
                .build()
        );
        setCustomPermissions(PermissionPreset.fromRole(702233714360582154L));
    }

    @Override
    public void process(GuildCommandEvent e) {
        JSONObject o = load("./Tierlists/518008523653775366.json");
        String str = e.getArguments().getText("mon");
        String mon = getDraftGerName(str).getTranslation();
        if (mon.isEmpty()) {
            e.reply("Das ist kein Pokemon!");
            return;
        }
        o.getJSONArray("trashmons").put(mon);
        save(o, "./Tierlists/518008523653775366.json");
        Tierlist.setup();
        e.reply("`%s` ist nun pickable!".formatted(mon));
    }
}
