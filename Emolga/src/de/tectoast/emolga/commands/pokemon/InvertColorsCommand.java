package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;

import java.io.File;

public class InvertColorsCommand extends Command {
    public InvertColorsCommand() {
        super("invertcolors", "Zeigt einen Sprite in der invertierten Farbe", CommandCategory.Pokemon);
        aliases.add("invertcolours");
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("shiny", "Shiny", "", ArgumentManagerTemplate.Text.of(SubCommand.of("Shiny", "Wenn das Pokemon als Shiny angezeigt werden soll")), true)
                .addEngl("mon", "Pokemon", "Das Pokemon lol", Translation.Type.POKEMON)
                .setExample("!invertcolors Zygarde")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        ArgumentManager args = e.getArguments();
        File f = invertImage(args.getTranslation("mon").getTranslation(), args.isText("shiny", "Shiny"));
        e.getChannel().sendFile(f).queue(message -> {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        });
    }
}
