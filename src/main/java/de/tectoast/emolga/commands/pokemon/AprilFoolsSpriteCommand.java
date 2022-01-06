package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;

import java.io.File;

public class AprilFoolsSpriteCommand extends Command {

    public AprilFoolsSpriteCommand() {
        super("aprilfoolsprite", "Zeigt den April-Fools-Sprite", CommandCategory.Pokemon);
        aliases.add("afd");
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("back", "Backsprite", "", ArgumentManagerTemplate.Text.of(
                        SubCommand.of("Front", "Wenn der Front-Sprite des Mons angezeigt werden soll (Standart)"),
                        SubCommand.of("Back", "Wenn der Back-Sprite des Mons angezeigt werden soll")), true)
                .add("shiny", "Shiny", "", ArgumentManagerTemplate.Text.of(SubCommand.of("Shiny", "Wenn der Sprite des Mons als Shiny angezeigt werden soll")), true)
                .add("form", "Form", "", ArgumentManagerTemplate.Text.of(
                        SubCommand.of("Alola"), SubCommand.of("Galar"), SubCommand.of("Mega")
                ), true)
                .addEngl("mon", "Pokemon", "Das Pokemon", Translation.Type.POKEMON)
                .setExample("!afd Galar Zigzachs")
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
        if(mon.equals("Popplio") || mon.equals("Primarina")) {
            e.reply("Möchte da jemand gebannt werden? :^)");
            return;
        }
        File f = new File("../Showdown/sspclient/sprites/afd"
                + (args.isText("back", "Back") ? "-back" : "")
                + (args.isText("shiny", "Shiny") ? "-shiny" : "")
                + "/" + mon.toLowerCase() + suffix + ".png");
        if (!f.exists()) {
            e.reply(mon + " hat keine " + args.getText("form") + "-Form!");
        }
        //if(!f.exists()) f = new File("../Showdown/sspclient/sprites/gen5-shiny/" + mon.split(";")[1].toLowerCase() + ".png");
        e.getChannel().sendFile(f).queue();
    }
}
