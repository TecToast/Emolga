package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.jsolf.JSONArray;
import de.tectoast.jsolf.JSONObject;

import java.io.File;

public class ShinyCommand extends Command {
    public ShinyCommand() {
        super("shiny", "Zeigt das Shiny des Pokemons an", CommandCategory.Pokemon);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("regform", "Form", "Optionale alternative Form", ArgumentManagerTemplate.Text.of(
                        SubCommand.of("Alola"), SubCommand.of("Galar"), SubCommand.of("Mega")
                ), true)
                .addEngl("mon", "Pokemon", "Das Mon, von dem das Shiny angezeigt werden soll", Translation.Type.POKEMON)
                .add("form", "Form", "Sonderform, bspw. `Heat` bei Rotom", ArgumentManagerTemplate.Text.any(), true)
                .setExample("!shiny Primarina")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        String suffix;
        ArgumentManager args = e.getArguments();
        String monname = e.getArguments().getTranslation("mon").getTranslation();
        JSONObject mon = getDataJSON().getJSONObject(toSDName(monname));
        if (args.has("regform")) {
            String form = args.getText("regform");
            suffix = "-" + form.toLowerCase();
        } else {
            suffix = "";
        }
        if(args.has("form")) {
            String form = args.getText("form");
            if (!mon.has("otherFormes")) {
                e.reply(monname + " besitzt keine **" + form + "**-Form!");
                return;
            }
            JSONArray otherFormes = mon.getJSONArray("otherFormes");
            if (otherFormes.toList().stream().noneMatch(s -> ((String) s).toLowerCase().endsWith("-" + form.toLowerCase()))) {
                e.reply(monname + " besitzt keine **" + form + "**-Form!");
                return;
            }
            if (suffix.isEmpty()) suffix = "-";
            suffix += form.toLowerCase();
        }
        File f = new File("../Showdown/sspclient/sprites/gen5-shiny/" + monname.toLowerCase() + suffix + ".png");
        if (!f.exists()) {
            e.reply(mon + " hat keine " + args.getText("form") + "-Form!");
        }
        //if(!f.exists()) f = new File("../Showdown/sspclient/sprites/gen5-shiny/" + mon.split(";")[1].toLowerCase() + ".png");
        e.getChannel().sendFile(f).queue();
    }
}
