package de.tectoast.emolga.commands.soullink;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.jsolf.JSONObject;

import java.util.List;

public class AddLocationCommand extends Command {

    public AddLocationCommand() {
        super("addlocation", "Fügt eine neue Location hinzu", CommandCategory.Soullink);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("location", "Location", "Die Location", ArgumentManagerTemplate.Text.any())
                .setExample("/addlocation Route 3")
                .build());
        slash(true, 695943416789598208L);
    }

    @Override
    public void process(GuildCommandEvent e) throws Exception {
        ArgumentManager args = e.getArguments();
        JSONObject soullink = getEmolgaJSON().getJSONObject("soullink");
        List<String> order = soullink.getStringList("order");
        String location = eachWordUpperCase(args.getText("location"));
        if (!order.contains(location)) {
            soullink.getJSONArray("order").put(location);
            e.reply("Die Location `%s` wurde eingetragen!".formatted(location));
            saveEmolgaJSON();
            updateSoullink();
            return;
        }
        e.reply("Die Location gibt es bereits!");
    }
}