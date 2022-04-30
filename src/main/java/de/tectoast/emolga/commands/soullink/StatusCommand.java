package de.tectoast.emolga.commands.soullink;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.jsolf.JSONObject;

public class StatusCommand extends Command {

    public StatusCommand() {
        super("status", "Setzt den Status eines Encounters", CommandCategory.Soullink);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("location", "Location", "Die Location", ArgumentManagerTemplate.Text.any())
                .add("status", "Status", "Der Status", ArgumentManagerTemplate.Text.of(SubCommand.of("Team"), SubCommand.of("Box"), SubCommand.of("RIP")))
                .setExample("/status Route 1 RIP")
                .build());
        slash(true, 695943416789598208L);
    }

    @Override
    public void process(GuildCommandEvent e) throws Exception {
        JSONObject soullink = getEmolgaJSON().getJSONObject("soullink");
        ArgumentManager args = e.getArguments();
        String location = eachWordUpperCase(args.getText("location"));
        if (!soullink.getStringList("order").contains(location)) {
            e.reply("Diese Location ist derzeit nicht im System!");
            return;
        }
        soullink.getJSONObject("mons").getJSONObject(location).put("status", args.getText("status"));
        e.reply("\uD83D\uDC4D");
        saveEmolgaJSON();
        updateSoullink();
    }
}
