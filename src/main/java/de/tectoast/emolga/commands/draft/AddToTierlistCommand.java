package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.Constants;
import de.tectoast.emolga.utils.draft.Tierlist;
import de.tectoast.jsolf.JSONObject;

public class AddToTierlistCommand extends Command {

    public AddToTierlistCommand() {
        super("addtotierlist", "Fügt ein Mon ins D-Tier ein", CommandCategory.Draft, Constants.ASLID);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("mon", "Mon", "Das Mon", ArgumentManagerTemplate.Text.any(), false)
                .add("tier", "Tier", "Das Tier, sonst das unterste", ArgumentManagerTemplate.Text.any(), true)
                .setExample("!addtotierlist Chimstix")
                .build()
        );
        setCustomPermissions(PermissionPreset.fromRole(702233714360582154L));
    }

    @Override
    public void process(GuildCommandEvent e) {
        String id = e.getGuild().getId();
        JSONObject o = load("./Tierlists/%s.json".formatted(id));
        String str = e.getArguments().getText("mon");
        String mon = getDraftGerName(str).getTranslation();
        if (mon.isEmpty()) {
            e.reply("Das ist kein Pokemon!");
            return;
        }
        if (e.getArguments().has("tier"))
            o.createOrGetJSON("additionalmons").createOrGetArray(e.getArguments().getText("tier")).put(mon);
        else
            o.getJSONArray("trashmons").put(mon);
        save(o, "./Tierlists/%s.json".formatted(id));
        Tierlist.setup();
        e.reply("`%s` ist nun pickable!".formatted(mon));
    }
}
