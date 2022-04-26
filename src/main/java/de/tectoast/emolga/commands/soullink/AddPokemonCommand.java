package de.tectoast.emolga.commands.soullink;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.jsolf.JSONObject;

import java.util.List;

public class AddPokemonCommand extends Command {

    public AddPokemonCommand() {
        super("addpokemon", "Fügt ein Pokemon hinzu", CommandCategory.Soullink);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("location", "Location", "Die Location", ArgumentManagerTemplate.Text.any())
                .add("pokemon", "Pokemon", "Das Pokemon", ArgumentManagerTemplate.draftPokemon())
                .add("status", "Status", "Der Status", ArgumentManagerTemplate.Text.of(
                        SubCommand.of("Team"),
                        SubCommand.of("Box"),
                        SubCommand.of("RIP")
                ))
                .setExample("/addpokemon Starter Robball Team")
                .build());
        slash();
    }

    @Override
    public void process(GuildCommandEvent e) throws Exception {
        ArgumentManager args = e.getArguments();
        JSONObject soullink = getEmolgaJSON().getJSONObject("soullink");
        List<String> order = soullink.getStringList("order");
        String pokemon = args.getText("pokemon");
        String location = eachWordUpperCase(args.getText("location"));
        if (!order.contains(location)) {
            soullink.getJSONArray("order").put(location);
        }
        JSONObject o = soullink.getJSONObject("mons").createOrGetJSON(location);
        o.put(soullinkIds.get(e.getAuthor().getIdLong()), pokemon);
        o.put("status", args.getText("status"));
        e.reply("\uD83D\uDC4D");
        saveEmolgaJSON();
        updateSoullink();
    }


}
