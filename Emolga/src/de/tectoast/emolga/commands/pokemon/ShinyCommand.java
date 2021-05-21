package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;

import java.io.File;

public class ShinyCommand extends Command {
    public ShinyCommand() {
        super("shiny", "Zeigt das Shiny des Pokemons an", CommandCategory.Pokemon);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("form", "Form", "Optionale alternative Form", ArgumentManagerTemplate.Text.of(
                        SubCommand.of("Alola"), SubCommand.of("Galar"), SubCommand.of("Mega")
                ), true)
                .addEngl("mon", "Pokemon", "Das Mon, von dem das Shiny angezeigt werden soll", Translation.Type.POKEMON)
                .setExample("!shiny Primarina")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        String suffix;
        ArgumentManager args = e.getArguments();
        if (args.has("form")) {
            String form = args.getText("form");
            suffix = "-" + form.toLowerCase();
        } else {
            suffix = "";
        }
        String mon = e.getArguments().getTranslation("mon").getTranslation();
        File f = new File("../Showdown/sspclient/sprites/gen5-shiny/" + mon.toLowerCase() + suffix + ".png");
        if (!f.exists()) {
            e.reply(mon + " hat keine " + args.getText("form") + "-Form!");
        }
        //if(!f.exists()) f = new File("../Showdown/sspclient/sprites/gen5-shiny/" + mon.split(";")[1].toLowerCase() + ".png");
        e.getChannel().sendFile(f).queue();
    }
}
